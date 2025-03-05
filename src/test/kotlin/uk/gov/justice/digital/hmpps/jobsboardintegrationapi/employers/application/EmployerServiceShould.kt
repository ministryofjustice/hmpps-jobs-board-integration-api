package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.InjectMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_CREATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_UPDATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.abcConstruction
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tescoLogistics
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_STATUS
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ServiceTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmployerServiceShould : ServiceTestCase() {
  @InjectMocks
  private lateinit var employerService: EmployerService

  private val employer = sainsburys
  private val externalId = 1L
  private val mnEmployer = employer.mnEmployer(externalId)
  private val mnEmployerNoId = mnEmployer.copy(id = null)

  @Nested
  @DisplayName("Given a valid employer")
  inner class GivenAnEmployer {
    @Test
    fun `return a valid employer`() {
      val id = employer.id
      whenever(jobsBoardApiClient.getEmployer(id)).thenReturn(employer)

      val actualEmployer = employerService.retrieveById(id)

      assertThat(actualEmployer).isNotNull.isEqualTo(employer)
    }

    @Test
    fun `NOT create ID mapping, when external ID exists`() {
      val id = employer.id
      givenEmployerExternalIDExists(id, 2)

      val exception = assertFailsWith<Exception> {
        employerService.createIdMapping(externalId, id)
      }

      assertThat(exception.message)
        .startsWith("Employer ID cannot be created! ID mapping already exists")
        .contains("id=$id").contains("externalId=$externalId")

      verify(employerExternalIdRepository, never()).save(any())
    }

    @Nested
    @DisplayName("And has not yet been registered at MN")
    inner class AndNotYetRegisteredAtMN {
      @Test
      fun `return false, when external ID does not exists`() {
        val id = employer.id
        givenEmployerExternalIDNotExist(id)

        employerService.existsIdMappingById(id)

        val exists = employerService.existsIdMappingById(id)
        assertThat(exists).isFalse()
      }

      @Test
      fun `convert new employer to MN`() {
        with(employer) { givenRefDataMappings(status, sector) }

        val actualMNEmployer = employerService.convert(employer)

        assertThat(actualMNEmployer).isEqualTo(mnEmployerNoId)
      }

      @Test
      fun `create employer at MN`() {
        CreateEmployerRequest.from(mnEmployerNoId).let {
          whenever(mnJobBoardApiClient.createEmployer(it)).thenReturn(mnEmployer)
        }

        val actualMNEmployer = employerService.create(mnEmployerNoId)

        assertThat(actualMNEmployer).isEqualTo(mnEmployer)
      }

      @Test
      fun `create ID mapping, when external ID does not exist`() {
        givenEmployerExternalIDNotExist(employer.id)
        val expectedEmployerId = employer.id
        val expectedExternalId = externalId

        employerService.createIdMapping(externalId, employer.id)

        val captor = argumentCaptor<EmployerExternalId>()
        verify(employerExternalIdRepository).save(captor.capture())
        val savedExternalId = captor.firstValue

        with(savedExternalId.key) {
          assertThat(id).isEqualTo(expectedEmployerId)
          assertThat(externalId).isEqualTo(expectedExternalId)
        }
      }

      @Test
      fun `NOT convert existing employer to MN, when external ID doest not exist`() {
        givenEmployerExternalIDNotExist(employer.id)

        val exception = assertFailsWith<IllegalStateException> {
          employerService.convertExisting(employer)
        }

        assertThat(exception.message).isEqualTo("Employer with id=${employer.id} not found (ID mapping missing)")
      }
    }

    @Nested
    @DisplayName("And has already been registered at MN")
    inner class AndAlreadyRegisteredAtMN {
      @Test
      fun `return true, when external ID exists`() {
        val id = employer.id
        givenEmployerExternalIDExists(id, externalId)

        val exists = employerService.existsIdMappingById(id)

        assertThat(exists).isTrue
      }

      @Test
      fun `convert existing employer to MN`() {
        givenEmployerExternalIDExists(employer.id, externalId)
        with(employer) { givenRefDataMappings(status, sector) }

        val actualMNEmployer = employerService.convertExisting(employer)

        assertThat(actualMNEmployer).isEqualTo(mnEmployer)
      }

      @Test
      fun `update employer at MN`() {
        val revisedEmployer = mnEmployer.copy(employerBio = "${mnEmployer.employerBio} |updated")
        UpdateEmployerRequest.from(revisedEmployer).let {
          whenever(mnJobBoardApiClient.updateEmployer(it)).thenReturn(revisedEmployer)
        }

        val actualMNEmployer = employerService.update(revisedEmployer)

        assertThat(actualMNEmployer).isEqualTo(revisedEmployer)
      }
    }
  }

  @Nested
  @DisplayName("Given a non-existent employer")
  inner class GivenNonExistentEmployer {
    @Test
    fun `NOT return a non-existent employer`() {
      val id = tesco.id
      whenever(jobsBoardApiClient.getEmployer(id)).thenReturn(null)

      val actualEmployer = employerService.retrieveById(id)

      assertThat(actualEmployer).isNull()
    }
  }

  @Nested
  @DisplayName("Given an invalid employer")
  inner class GivenInvalidEmployer {
    @Test
    fun `NOT convert employer to MN, with invalid status`() {
      val refData = EMPLOYER_STATUS.type
      val invalidStatus = "INVALID"
      val invalidEmployer = employer.copy(
        id = randomUUID(),
        status = invalidStatus,
      )
      with(invalidEmployer) { givenRefDataMappings(status, sector) }

      val exception = assertFailsWith<IllegalArgumentException> {
        employerService.convert(invalidEmployer)
      }

      assertThat(exception.message)
        .startsWith("Reference data does not exist!").contains("refData=$refData: value=$invalidStatus")
    }

    @Test
    fun `NOT convert employer to MN, with invalid sector`() {
      val refData = EMPLOYER_SECTOR.type
      val invalidSector = "INVALID"
      val invalidEmployer = employer.copy(
        id = randomUUID(),
        sector = invalidSector,
      )
      givenRefDataIdMapping(EMPLOYER_SECTOR.type, invalidEmployer.sector)

      val exception = assertFailsWith<IllegalArgumentException> {
        employerService.convert(invalidEmployer)
      }

      assertThat(exception.message)
        .startsWith("Reference data does not exist!").contains("refData=$refData: value=$invalidSector")
    }
  }

  @Nested
  @DisplayName("Given some existing employers, but not yet registered with MN")
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class GivenExistingEmployersYetRegistered {
    private lateinit var employerIds: List<String>
    private lateinit var employerCreationEvents: Array<EmployerEvent>
    private lateinit var employerUpdateEvents: Array<EmployerEvent>

    @BeforeEach
    fun setUp() {
      employerIds = listOf(tesco, amazon, sainsburys).map { it.id }.toList()
      employerCreationEvents = employerIds.map { makeEvent(it) }.toTypedArray()
      employerUpdateEvents = listOf(abcConstruction, tescoLogistics).map { makeEvent(it.id, EMPLOYER_UPDATED) }.toTypedArray()
    }

    @Test
    @Order(1)
    fun `send a few events`() {
      val itemCount = employerService.sendEvent(*employerCreationEvents)
      assertEquals(employerIds.size, itemCount)
    }

    @Test
    @Order(2)
    fun `proceed sending subsequent event, after a failure`() {
      val errorEventIds = (0..1).map { i -> employerCreationEvents[i].eventId }.toSet()
      whenever(eventEmitter.send(any<EventData>())).thenAnswer {
        val eventId = (it.arguments.first() as EventData).eventId
        if (errorEventIds.contains(eventId)) throw RuntimeException("Unexpected failure")
      }

      val itemCount = employerService.sendEvent(*employerCreationEvents)
      assertEquals(1, itemCount)
    }

    @Test
    @Order(3)
    fun `send events of different types`() {
      val expectedCount = employerCreationEvents.size + employerUpdateEvents.size

      val itemCount = employerService.sendEvent(*(employerCreationEvents + employerUpdateEvents))
      assertEquals(expectedCount, itemCount)
    }

    @Test
    @Order(11)
    fun `send more events`() {
      val count = 10_000
      val events = (1..count).map { makeEvent(randomUUID()) }.toTypedArray()
      whenever(eventEmitter.send(any<EventData>())).then { runBlocking { delay(1L) } }

      val itemCount = employerService.sendEvent(*events)
      assertEquals(count, itemCount)
    }
  }

  private fun givenEmployerExternalIDExists(id: String, externalId: Long) {
    val employerExternalId = EmployerExternalId(id, externalId)
    whenever(employerExternalIdRepository.findByKeyId(id)).thenReturn(employerExternalId)
  }

  private fun givenEmployerExternalIDNotExist(id: String) {
    whenever(employerExternalIdRepository.findByKeyId(id)).thenReturn(null)
  }

  private fun givenRefDataMappings(employerStatus: String, employerSector: String) {
    givenRefDataIdMapping(EMPLOYER_STATUS.type, employerStatus)
    givenRefDataIdMapping(EMPLOYER_SECTOR.type, employerSector)
  }

  private fun makeEvent(id: String, eventType: EmployerEventType = EMPLOYER_CREATED) = EmployerEvent(
    eventId = randomUUID(),
    eventType = eventType,
    timestamp = defaultCurrentTime,
    employerId = id,
  )
}
