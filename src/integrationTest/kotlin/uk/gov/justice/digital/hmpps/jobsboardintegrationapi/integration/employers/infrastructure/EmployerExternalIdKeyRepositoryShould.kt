package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.employers.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure.RepositoryTestCase
import kotlin.test.assertFailsWith

class EmployerExternalIdKeyRepositoryShould : RepositoryTestCase() {

  @Test
  fun `return empty list, when nothing has been created yet`() {
    employerExternalIdRepository.findAll().let {
      assertThat(it).isEmpty()
    }
  }

  @Test
  fun `return nothing, for any employer ID`() {
    val employerId = randomId()
    val actual = employerExternalIdRepository.findByKeyId(employerId)
    assertThat(actual).isNull()
  }

  @Test
  fun `return nothing, for any employer external ID`() {
    val externalId = randomExtId()

    val actual = employerExternalIdRepository.findByKeyExternalId(externalId)
    assertThat(actual).isNull()
  }

  @Nested
  @DisplayName("Given an employer has been created")
  inner class GivenAnEmployer {
    private val employer = sainsburys
    private val expectedExtId = randomExtId()
    private val externalId = EmployerExternalId(employer.id, expectedExtId)

    @Nested
    @DisplayName("And no external ID mapping has been created, for the given employer")
    inner class AndNoExternalIdMapping {
      @Test
      fun `return nothing, for given employer ID`() {
        val actual = employerExternalIdRepository.findByKeyId(employer.id)
        assertThat(actual).isNull()
      }

      @Test
      fun `create external ID mapping`() {
        val savedExternalId = employerExternalIdRepository.save(externalId)

        assertThat(savedExternalId.key.id).isEqualTo(employer.id)
        assertThat(savedExternalId.key.externalId).isEqualTo(expectedExtId)
        assertThat(savedExternalId).isEqualTo(externalId)
      }

      @Test
      fun `record timestamps, when creating external ID mapping`() {
        val savedExternalId = employerExternalIdRepository.save(externalId)
        with(savedExternalId) {
          assertThat(this.createdAt).isEqualTo(currentTime)
          assertThat(this.lastModifiedAt).isEqualTo(currentTime)
        }
      }
    }

    @Nested
    @DisplayName("And external ID mapping has been created, for the given employer")
    inner class AndExternalIdMappingCreated {

      @BeforeEach
      internal fun setUp() {
        employerExternalIdRepository.saveAndFlush(externalId)
      }

      @Test
      fun `return employer external ID mapping, for given employer ID`() {
        val actual = employerExternalIdRepository.findByKeyId(employer.id)
        assertThat(actual).isNotNull.isEqualTo(externalId)
      }

      @Test
      fun `return employer external ID mapping, for given employer external ID`() {
        val actual = employerExternalIdRepository.findByKeyExternalId(expectedExtId)
        assertThat(actual).isNotNull.isEqualTo(externalId)
      }

      @Test
      fun `throw error, when creating with duplicate ID`() {
        val newExternalId = EmployerExternalId(employer.id, randomExtId())

        val exception = assertFailsWith<DataIntegrityViolationException> {
          employerExternalIdRepository.saveAndFlush(newExternalId)
        }

        assertThat(exception.cause).isInstanceOfAny(ConstraintViolationException::class.java)
        exception.cause!!.message!!.let {
          assertThat(it)
            .contains("ERROR: duplicate key value violates unique constraint")
            .contains(employer.id)
        }
      }
    }
  }

  @Nested
  @DisplayName("Given two employers have been created")
  inner class GivenTwoEmployers {
    private val employerA = sainsburys
    private val externalIdOfEmployerA = randomExtId()
    private val employerB = tesco

    private val employerExternalIdOfA = EmployerExternalId(employerA.id, externalIdOfEmployerA)

    @BeforeEach
    internal fun setUp() {
      employerExternalIdRepository.saveAndFlush(employerExternalIdOfA)
    }

    @Test
    fun `return nothing, for the employer without external ID mapped`() {
      val actual = employerExternalIdRepository.findByKeyId(employerB.id)
      assertThat(actual).isNull()
    }

    @Test
    fun `throw error, when creating with duplicate external ID`() {
      val newExternalId = EmployerExternalId(employerB.id, externalIdOfEmployerA)

      val exception = assertFailsWith<DataIntegrityViolationException> {
        employerExternalIdRepository.saveAndFlush(newExternalId)
      }

      assertThat(exception.cause).isInstanceOfAny(ConstraintViolationException::class.java)
      exception.cause!!.message!!.let {
        assertThat(it)
          .contains("ERROR: duplicate key value violates unique constraint")
          .contains(externalIdOfEmployerA.toString())
      }
    }

    @Test
    fun `create external ID mapping, without any duplicate ID or external ID`() {
      val newExtId = randomExtId()
      val newExternalId = EmployerExternalId(employerB.id, newExtId)

      val savedExternalId = employerExternalIdRepository.saveAndFlush(newExternalId)

      assertThat(savedExternalId.key.id).isEqualTo(employerB.id)
      assertThat(savedExternalId.key.externalId).isEqualTo(newExtId)
      assertThat(savedExternalId).isEqualTo(newExternalId)
    }
  }
}
