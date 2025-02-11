package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_STATUS
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ServiceTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest
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
}
