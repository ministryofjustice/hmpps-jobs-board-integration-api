package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.abcConstructionApprentice
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.JobsBoardApiWebClient.Companion.employerResponseTypeRef
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.JobsBoardApiWebClient.Companion.jobResponseTypeRef

@ExtendWith(MockitoExtension::class)
class JobsBoardApiWebClientShould : JobsBoardApiWebClientTestBase() {

  @Nested
  inner class GivenAnEmployer {
    private val employer = sainsburys.makeCopy()
    private val someEmployers = arrayOf(sainsburys, tesco, amazon)
    private val baseUri = "/employers"
    private val uri = "$baseUri/{id}"

    @Test
    fun `return employer details with a valid employer ID`() {
      replyOnGetEmployerById(GetEmployerResponse.from(employer), employer.id)

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employer.id)

      assertThat(actualEmployer).isEqualTo(employer)
    }

    @Test
    fun `return all employers`() {
      val responses = someEmployers.map { GetEmployerResponse.from(it) }.toTypedArray()
      replyOnGetEmployers(GetEmployersResponse.from(*responses))
      val employersPage = jobsBoardApiWebClient.getAllEmployers()

      assertThat(employersPage.content)
        .hasSize(someEmployers.size)
        .contains(*responses)
    }

    private fun replyOnGetEmployerById(response: GetEmployerResponse, employerId: String) = replyOnRequestById(response.javaClass, response, uri, employerId)

    private fun replyOnGetEmployers(response: GetEmployersResponse) = replyOnPagedRequest(employerResponseTypeRef, response, baseUri)
  }

  @Nested
  inner class GivenAnEmployerWithAJob {
    private val job = tescoWarehouseHandler.makeCopy()
    private val uri = "/jobs/{id}"

    @Test
    fun `return job details with a valid job ID`() {
      replyOnGetJobById(GetJobResponse.from(job), job.id)

      val actualJob = jobsBoardApiWebClient.getJob(job.id)

      assertThat(actualJob).isEqualTo(job)
    }

    @Test
    fun `return all jobs`() {
      val response = GetJobsData.from(job)
      replyOnGetJobs(GetJobsResponse.from(response))

      val jobPage = jobsBoardApiWebClient.getAllJobs()

      assertThat(jobPage.content)
        .hasSize(1)
        .contains(response)
    }

    @Nested
    @DisplayName("And more jobs")
    inner class AndMoreJobs {
      val jobs = listOf(job, amazonForkliftOperator, abcConstructionApprentice).map { it.makeCopy() }

      @Test
      fun `return all jobs`() {
        val responses = jobs.map { GetJobsData.from(it) }.toTypedArray()
        replyOnGetJobs(GetJobsResponse.from(*responses))

        val jobPage = jobsBoardApiWebClient.getAllJobs()

        assertThat(jobPage.content)
          .hasSize(responses.size)
          .contains(*responses)
      }
    }

    private fun replyOnGetJobById(response: GetJobResponse, jobId: String) = replyOnRequestById(response.javaClass, response, uri, jobId)

    private fun replyOnGetJobs(response: GetJobsResponse) = replyOnPagedRequest(jobResponseTypeRef, response, uri)
  }
}
