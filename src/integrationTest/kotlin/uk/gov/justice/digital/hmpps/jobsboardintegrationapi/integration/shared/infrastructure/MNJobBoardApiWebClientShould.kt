package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MNJobBoardApiExtension.Companion.mnJobBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.mnJob
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJobBoardApiWebClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateJobRequest
import kotlin.test.assertFailsWith

class MNJobBoardApiWebClientShould : IntegrationTestBase() {
  @Autowired
  private lateinit var apiWebClient: MNJobBoardApiWebClient

  private val employer = sainsburys
  private val mnEmployer: MNEmployer get() = employer.copy(createdAt = timeProvider.nowAsInstant()).mnEmployer()

  private val job = tescoWarehouseHandler
  private val mnJob: MNJob get() = job.copy(createdAt = timeProvider.nowAsInstant()).mnJob()

  @Nested
  @DisplayName("MN JobBoard `POST` /employers ; create employer")
  inner class EmployersPostEndpoint {
    @Test
    fun `create employer, with valid details`() {
      val expectedEmployer = mnEmployer.copy(id = 1L)
      mnJobBoardApi.stubCreateEmployer(mnEmployer)

      val actualEmployer = CreateEmployerRequest.from(mnEmployer).let { apiWebClient.createEmployer(it) }

      assertThat(actualEmployer).isEqualTo(expectedEmployer)
    }

    @Test
    fun `receive unauthorised error, if API access token is invalid`() {
      mnJobBoardApi.stubCreateEmployerUnauthorised()

      val exception = assertFailsWith<Exception> {
        CreateEmployerRequest.from(mnEmployer).let { apiWebClient.createEmployer(it) }
      }

      with(exception) {
        assertThat(message).contains("Fail to create employer") // reactive throw (ReactiveException)
        with(cause!!) {
          assertThat(message).contains("Fail to create employer") // actual throw (Exception)
          with(cause!!) {
            assertThat(message).contains("401 Unauthorized") // cause (401) (WebClientResponseException$Unauthorized)
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("MN JobBoard `POST` /employers/{id} ; update employer")
  inner class EmployerPostEndpoint {
    private val existingEmployer = with(mnEmployer) { copy(id = 1L, employerBio = "$employerBio |updated") }

    @Test
    fun `update employer, with valid details`() {
      val expectedEmployer = existingEmployer.copy()
      mnJobBoardApi.stubUpdateEmployer(existingEmployer)

      val actualEmployer = UpdateEmployerRequest.from(existingEmployer).let { apiWebClient.updateEmployer(it) }

      assertThat(actualEmployer).isEqualTo(expectedEmployer)
    }

    @Test
    fun `receive unauthorised error, if API access token is invalid`() {
      mnJobBoardApi.stubUpdateEmployerUnauthorised(existingEmployer.id!!)

      val exception = assertFailsWith<Exception> {
        UpdateEmployerRequest.from(existingEmployer).let { apiWebClient.updateEmployer(it) }
      }

      with(exception) {
        assertThat(message).contains("Fail to update employer") // reactive throw (ReactiveException)
        with(cause!!) {
          assertThat(message).contains("Fail to update employer") // actual throw (Exception)
          with(cause!!) {
            assertThat(message).contains("401 Unauthorized") // cause (401) (WebClientResponseException$Unauthorized)
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("MN JobBoard `POST` /jobs-prison-leavers ; create job")
  inner class JobsPostEndpoint {
    @Test
    fun `create job, with valid details`() {
      val expectedJob = mnJob.copy(id = 1L)
      mnJobBoardApi.stubCreateJob(expectedJob, expectedJob.id!!)

      val actualJob = CreateJobRequest.from(mnJob).let { apiWebClient.createJob(it) }

      assertThat(actualJob).isEqualTo(expectedJob)
    }

    @Test
    fun `receive unauthorised error, if API access token is invalid`() {
      mnJobBoardApi.stubCreateJobUnauthorised()

      val exception = assertFailsWith<Exception> {
        CreateJobRequest.from(mnJob).let { apiWebClient.createJob(it) }
      }

      with(exception) {
        assertThat(message).contains("Fail to create job") // reactive throw (ReactiveException)
        with(cause!!) {
          assertThat(message).contains("Fail to create job") // actual throw (Exception)
          with(cause!!) {
            assertThat(message).contains("401 Unauthorized") // cause (401) (WebClientResponseException$Unauthorized)
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("MN JobBoard `PUT` /jobs-prison-leavers ; update job")
  inner class JobPutEndpoint {
    @Test
    fun `update job, with valid details`() {
      val expectedJob = mnJob.copy(id = 101L)
      mnJobBoardApi.stubUpdateJob(expectedJob)

      val actualJob = UpdateJobRequest.from(expectedJob).let { apiWebClient.updateJob(it) }

      assertThat(actualJob).isEqualTo(expectedJob)
    }

    @Test
    fun `receive unauthorised error, if API access token is invalid`() {
      val expectedJob = mnJob.copy(id = 102L)
      mnJobBoardApi.stubUpdateJobUnauthorised()

      val exception = assertFailsWith<Exception> {
        UpdateJobRequest.from(expectedJob).let { apiWebClient.updateJob(it) }
      }

      with(exception) {
        assertThat(message).contains("Fail to update job") // reactive throw (ReactiveException)
        with(cause!!) {
          assertThat(message).contains("Fail to update job") // actual throw (Exception)
          with(cause!!) {
            assertThat(message).contains("401 Unauthorized") // cause (401) (WebClientResponseException$Unauthorized)
          }
        }
      }
    }
  }
}
