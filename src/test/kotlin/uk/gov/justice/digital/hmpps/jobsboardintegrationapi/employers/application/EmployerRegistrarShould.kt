package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import kotlin.test.assertFailsWith

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

  private fun givenMNEmployerToCreate(employer: Employer, convertedEmployer: MNEmployer, mnEmployer: MNEmployer) {
    whenever(employerService.existsIdMappingById(employer.id)).thenReturn(false)
    whenever(employerService.convert(employer)).thenReturn(convertedEmployer)
    whenever(employerService.create(convertedEmployer)).thenReturn(mnEmployer)
  }
}
