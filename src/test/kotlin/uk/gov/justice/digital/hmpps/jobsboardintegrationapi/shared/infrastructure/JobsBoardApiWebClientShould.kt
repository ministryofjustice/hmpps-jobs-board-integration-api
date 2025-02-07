package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler

@ExtendWith(MockitoExtension::class)
class JobsBoardApiWebClientShould : JobsBoardApiWebClientTestBase() {

  @Nested
  inner class GivenAnEmployer {
    private val employer = sainsburys.copy(createdAt = defaultCurrentTime)
    private val uri = "/employers/{id}"

    @Test
    fun `return employer details with a valid employer ID`() {
      replyOnGetEmployerById(GetEmployerResponse.from(employer), employer.id)

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employer.id)

      assertThat(actualEmployer).isEqualTo(employer)
    }

    private fun replyOnGetEmployerById(response: GetEmployerResponse, employerId: String) =
      replyOnRequestById(response.javaClass, response, uri, employerId)
  }

  @Nested
  inner class GivenAnEmployerWithAJob {
    private val job = tescoWarehouseHandler.copy(createdAt = defaultCurrentTime)
    private val uri = "/jobs/{id}"

    @Test
    fun `return job details with a valid job ID`() {
      replyOnGetJobById(GetJobResponse.from(job), job.id)

      val actualJob = jobsBoardApiWebClient.getJob(job.id)

      assertThat(actualJob).isEqualTo(job)
    }

    private fun replyOnGetJobById(response: GetJobResponse, jobId: String) =
      replyOnRequestById(response.javaClass, response, uri, jobId)
  }
}
