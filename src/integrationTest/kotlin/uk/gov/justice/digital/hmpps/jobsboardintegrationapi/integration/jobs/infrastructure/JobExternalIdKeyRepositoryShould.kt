package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.jobs.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure.RepositoryTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import kotlin.test.assertFailsWith

class JobExternalIdKeyRepositoryShould : RepositoryTestCase() {

  @Test
  fun `return empty list, when nothing has been created yet`() {
    jobExternalIdRepository.findAll().let {
      assertThat(it).isEmpty()
    }
  }

  @Test
  fun `return nothing, for any job ID`() {
    val jobId = randomId()
    val actual = jobExternalIdRepository.findByKeyId(jobId)
    assertThat(actual).isNull()
  }

  @Test
  fun `return nothing, for any job external ID`() {
    val externalId = randomExtId()

    val actual = jobExternalIdRepository.findByKeyExternalId(externalId)
    assertThat(actual).isNull()
  }

  @Nested
  @DisplayName("Given a job has been created")
  inner class GivenAJob {
    private val job = tescoWarehouseHandler
    private val expectedExtId = randomExtId()
    private val externalId = JobExternalId(job.id, expectedExtId)

    @Nested
    @DisplayName("And no external ID mapping has been created, for the given job")
    inner class AndNoExternalIdMapping {
      @Test
      fun `return nothing, for given job ID`() {
        val actual = jobExternalIdRepository.findByKeyId(job.id)
        assertThat(actual).isNull()
      }

      @Test
      fun `create external ID mapping`() {
        val savedExternalId = jobExternalIdRepository.save(externalId)

        assertThat(savedExternalId.key.id).isEqualTo(job.id)
        assertThat(savedExternalId.key.externalId).isEqualTo(expectedExtId)
        assertThat(savedExternalId).isEqualTo(externalId)
      }

      @Test
      fun `record timestamps, when creating external ID mapping`() {
        val savedExternalId = jobExternalIdRepository.save(externalId)
        with(savedExternalId) {
          assertThat(this.createdAt).isEqualTo(currentTime)
          assertThat(this.lastModifiedAt).isEqualTo(currentTime)
        }
      }
    }

    @Nested
    @DisplayName("And external ID mapping has been created, for the given job")
    inner class AndExternalIdMappingCreated {

      @BeforeEach
      internal fun setUp() {
        jobExternalIdRepository.saveAndFlush(externalId)
      }

      @Test
      fun `return job external ID mapping, for given job ID`() {
        val actual = jobExternalIdRepository.findByKeyId(job.id)
        assertThat(actual).isNotNull.isEqualTo(externalId)
      }

      @Test
      fun `return job external ID mapping, for given job external ID`() {
        val actual = jobExternalIdRepository.findByKeyExternalId(expectedExtId)
        assertThat(actual).isNotNull.isEqualTo(externalId)
      }

      @Test
      fun `throw error, when creating with duplicate ID`() {
        val newExternalId = JobExternalId(job.id, randomExtId())

        val exception = assertFailsWith<DataIntegrityViolationException> {
          jobExternalIdRepository.saveAndFlush(newExternalId)
        }

        assertThat(exception.cause).isInstanceOfAny(ConstraintViolationException::class.java)
        exception.cause!!.message!!.let {
          assertThat(it)
            .contains("ERROR: duplicate key value violates unique constraint")
            .contains(job.id)
        }
      }
    }
  }

  @Nested
  @DisplayName("Given two jobs have been created")
  inner class GivenTwoJobs {
    private val jobA = tescoWarehouseHandler
    private val externalIdOfJobA = randomExtId()
    private val jobB = amazonForkliftOperator

    private val jobExternalIdOfA = JobExternalId(jobA.id, externalIdOfJobA)

    @BeforeEach
    internal fun setUp() {
      jobExternalIdRepository.saveAndFlush(jobExternalIdOfA)
    }

    @Test
    fun `return nothing, for the job without external ID mapped`() {
      val actual = jobExternalIdRepository.findByKeyId(jobB.id)
      assertThat(actual).isNull()
    }

    @Test
    fun `throw error, when creating with duplicate external ID`() {
      val newExternalId = JobExternalId(jobB.id, externalIdOfJobA)

      val exception = assertFailsWith<DataIntegrityViolationException> {
        jobExternalIdRepository.saveAndFlush(newExternalId)
      }

      assertThat(exception.cause).isInstanceOfAny(ConstraintViolationException::class.java)
      exception.cause!!.message!!.let {
        assertThat(it)
          .contains("ERROR: duplicate key value violates unique constraint")
          .contains(externalIdOfJobA.toString())
      }
    }

    @Test
    fun `create external ID mapping, without any duplicate ID or external ID`() {
      val newExtId = randomExtId()
      val newExternalId = JobExternalId(jobB.id, newExtId)

      val savedExternalId = jobExternalIdRepository.saveAndFlush(newExternalId)

      assertThat(savedExternalId.key.id).isEqualTo(jobB.id)
      assertThat(savedExternalId.key.externalId).isEqualTo(newExtId)
      assertThat(savedExternalId).isEqualTo(newExternalId)
    }
  }
}
