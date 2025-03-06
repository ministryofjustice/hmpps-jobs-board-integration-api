package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.abcConstruction
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tescoLogistics
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient.Companion.FETCH_SIZE as API_FETCH_SIZE

class EmployerRegistrarShould : UnitTestBase() {
  @Mock
  private lateinit var employerService: EmployerService

  @InjectMocks
  private lateinit var employerRegistrar: EmployerRegistrar

  @Nested
  @DisplayName("Given a new employer to be created at MN")
  inner class GivenAnEmployerToCreate {
    private val employer = sainsburys
    private val externalId = 1L
    private val mnEmployer = employer.mnEmployer(externalId)
    private val mnEmployerNoId = mnEmployer.copy(id = null)

    @Test
    fun `register new employer`() {
      givenMNEmployerToCreate(employer, mnEmployerNoId, mnEmployer)

      employerRegistrar.registerCreation(employer)

      verify(employerService).createIdMapping(externalId, employer.id)
    }

    @Test
    fun `skip registering existing employer`() {
      val id = employer.id
      whenever(employerService.existsIdMappingById(id)).thenReturn(true)

      employerRegistrar.registerCreation(employer)

      verify(employerService, never()).convert(employer)
      verify(employerService, never()).create(mnEmployerNoId)
      verify(employerService, never()).createIdMapping(externalId, id)
    }

    @Test
    fun `NOT finish registering employer without external ID received`() {
      givenMNEmployerToCreate(employer, mnEmployerNoId, mnEmployerNoId)

      val exception = assertFailsWith<Exception> {
        employerRegistrar.registerCreation(employer)
      }

      assertThat(exception.message)
        .startsWith("Fail to register employer-creation").contains("employerId=${employer.id}")
      with(exception.cause) {
        assertThat(this).isNotNull.isInstanceOfAny(AssertionError::class.java)
        assertThat(this!!.message)
          .startsWith("MN Employer ID is missing!").contains("employerId=${employer.id}")
      }
      verify(employerService, never()).createIdMapping(externalId, employer.id)
    }

    @Test
    fun `NOT finish registering employer, with ID mapping clash while saving it`() {
      whenever(employerService.existsIdMappingById(employer.id)).thenReturn(false, true)
      whenever(employerService.convert(employer)).thenReturn(mnEmployerNoId)
      whenever(employerService.create(mnEmployerNoId)).thenReturn(mnEmployer)
      whenever(employerService.createIdMapping(externalId, employer.id)).thenCallRealMethod()

      val exception = assertFailsWith<Exception> {
        employerRegistrar.registerCreation(employer)
      }

      assertThat(exception.message)
        .startsWith("Fail to register employer-creation").contains("employerId=${employer.id}")
    }
  }

  @Nested
  @DisplayName("Given an existing employer to be updated to MN")
  inner class GivenAnEmployerToUpdate {
    private val employer = sainsburys.run { copy(description = "$description |updated") }
    private val externalId = 1L
    private val mnEmployer = employer.mnEmployer(externalId)

    @Test
    fun `update a registered employer`() {
      givenMNEmployerToUpdate(employer, mnEmployer)

      employerRegistrar.registerUpdate(employer)

      verify(employerService, never()).createIdMapping(externalId, employer.id)
    }

    @Test
    fun `throw exception, when external ID has been changed unexpectedly`() {
      givenMNEmployerToUpdate(employer, mnEmployer, mnEmployer.copy(id = 999L))

      val exception = assertFailsWith<Exception> {
        employerRegistrar.registerUpdate(employer)
      }

      with(exception) {
        assertThat(message).startsWith("Fail to register employer-update").contains("employerId=${employer.id}")
        with(cause!!) {
          assertThat(this).isInstanceOfAny(AssertionError::class.java)
          assertThat(message).startsWith("MN Employer ID has changed!").contains("employerId=${employer.id}")
        }
      }
    }

    @Test
    fun `throw exception, with error from conversion`() {
      val expectedException = IllegalStateException("Employer with id=${employer.id} not found (ID mapping missing)")
      whenever(employerService.convertExisting(employer)).thenThrow(expectedException)

      val actualException = assertFailsWith<Exception> {
        employerRegistrar.registerUpdate(employer)
      }

      with(actualException) {
        assertThat(message).startsWith("Fail to register employer-update").contains("employerId=${employer.id}")
        with(cause!!) {
          assertThat(this).isInstanceOfAny(expectedException.javaClass)
          assertThat(message).isEqualTo(expectedException.message)
        }
      }
    }

    @Test
    fun `throw exception, with error from updating downstream system`() {
      val expectedException = "some errors while updating".let {
        RuntimeException("Fail to update employer! errorResponse=$it", RuntimeException(it))
      }
      whenever(employerService.convertExisting(employer)).thenReturn(mnEmployer)
      whenever(employerService.update(mnEmployer)).thenThrow(expectedException)

      val actualException = assertFailsWith<Throwable> {
        employerRegistrar.registerUpdate(employer)
      }

      with(actualException) {
        assertThat(message).startsWith("Fail to register employer-update").contains("employerId=${employer.id}")
        with(cause!!) {
          assertThat(message).isEqualTo(expectedException.message)
        }
      }
    }
  }

  @Nested
  @DisplayName("Given some missing employers to be created")
  inner class GivenMissingEmployersToCreate {
    private val allEmployers = listOf(tesco, amazon, tescoLogistics, abcConstruction)
    private val missingEmployerIds = listOf(tesco, amazon).map { it.id }

    @BeforeEach
    internal fun setUp() {
      whenever(employerService.sendEvent(anyVararg(EmployerEvent::class))).thenAnswer { it.arguments.size }
    }

    @Test
    fun `discover and resend missing employers`() {
      givenEmployersToResend()
      givenMissingEmployers()

      var sentCount: Long
      var totalCount: Long
      employerRegistrar.discoverAndResend().run {
        sentCount = first
        totalCount = second
      }

      assertEquals(allEmployers.size.toLong(), totalCount)
      assertEquals(missingEmployerIds.size.toLong(), sentCount)
      verify(employerService).sendEvent(anyVararg(EmployerEvent::class))
      verify(employerService).retrieveAll(0, FETCH_SIZE)
    }

    @Test
    fun `discover and resend missing employers, with paginated results`() {
      val fetchSize = 2
      givenEmployersToResend(fetchSize)
      givenMissingEmployers()

      val sentCount = employerRegistrar.discoverAndResend(fetchSize).run { first }

      assertEquals(missingEmployerIds.size.toLong(), sentCount)
      verify(employerService, times(1)).sendEvent(anyVararg(EmployerEvent::class))
      repeat(2) { page -> verify(employerService).retrieveAll(page, fetchSize) }
    }

    @Test
    fun `discover and resend missing employers, with paginated results (single record per page)`() {
      val fetchSize = 1
      givenEmployersToResend(fetchSize)
      givenMissingEmployers()

      val sentCount = employerRegistrar.discoverAndResend(fetchSize).run { first }

      assertEquals(missingEmployerIds.size.toLong(), sentCount)
      verify(employerService, times(2)).sendEvent(anyVararg(EmployerEvent::class))
      repeat(4) { page -> verify(employerService).retrieveAll(page, fetchSize) }
    }

    @Test
    fun `resend employers, with given employer IDs`() {
      val employerIds = missingEmployerIds + abcConstruction.id
      givenMissingEmployers()

      val sentCount = employerRegistrar.resend(employerIds)

      assertEquals(missingEmployerIds.size, sentCount)
    }

    @Test
    fun `resend employers, with given employer IDs forcing update`() {
      val employerIds = allEmployers.map { it.id }
      givenMissingEmployers()

      val sentCount = employerRegistrar.resend(employerIds, forceUpdate = true)

      assertEquals(employerIds.size, sentCount)
    }

    private fun givenEmployersToResend() = givenAllEmployers(allEmployers)
    private fun givenEmployersToResend(pageSize: Int) = givenAllEmployers(allEmployers, pageSize)
    private fun givenMissingEmployers() = whenever(employerService.existsIdMappingById(anyString())).thenAnswer { !missingEmployerIds.contains(it.arguments.first()) }
  }

  private fun givenMNEmployerToCreate(employer: Employer, convertedEmployer: MNEmployer, mnEmployer: MNEmployer) {
    whenever(employerService.existsIdMappingById(employer.id)).thenReturn(false)
    whenever(employerService.convert(employer)).thenReturn(convertedEmployer)
    whenever(employerService.create(convertedEmployer)).thenReturn(mnEmployer)
  }

  private fun givenMNEmployerToUpdate(
    employer: Employer,
    mnEmployer: MNEmployer,
    updatedEmployer: MNEmployer = mnEmployer,
  ) {
    whenever(employerService.convertExisting(employer)).thenReturn(mnEmployer)
    whenever(employerService.update(mnEmployer)).thenReturn(updatedEmployer)
  }

  private fun givenAllEmployers(employers: List<Employer>, pageSize: Int = API_FETCH_SIZE) {
    val totalCount = employers.size
    whenever(employerService.retrieveAll(anyInt(), anyInt())).thenAnswer {
      val page = it.arguments.first() as Int
      val range = (page * pageSize).let { offset -> IntRange(offset, min(offset + pageSize, totalCount) - 1) }
      val items = employers.slice(range).toTypedArray()
      GetEmployersResponse.from(*items, number = page, size = pageSize, totalElements = totalCount.toLong())
    }
  }
}
