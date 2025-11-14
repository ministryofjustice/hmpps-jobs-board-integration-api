package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.application.ApplicationTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension.Companion.jobsBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.getEmployersResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.getJobsResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.abcConstructionApprentice
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.nationalTescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobsData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.JobsBoardApiWebClient
import kotlin.math.ceil

class JobsBoardApiWebClientShould : ApplicationTestCase() {

  @Autowired
  private lateinit var jobsBoardApiWebClient: JobsBoardApiWebClient

  @Nested
  @DisplayName("JobsBoard `GET` /employers/{id}")
  inner class EmployersGetDetailsEndpoint {
    @Test
    fun `return employer details, given valid employer ID`() {
      val employer = sainsburys.makeCopy()

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveEmployer(employer)

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employer.id)

      assertThat(actualEmployer).isEqualTo(employer)
    }

    @Test
    fun `return nothing, given invalid employer ID`() {
      val employerId = randomUUID()

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveEmployerNotFound()

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employerId)

      assertThat(actualEmployer).isNull()
    }
  }

  @Nested
  @DisplayName("JobsBoard `GET` /employers")
  inner class EmployersRetrieveAllEndpoint {
    private val someEmployers =
      arrayOf(sainsburys, tesco, amazon).map { it.makeCopy() }.toTypedArray()

    @Test
    fun `return all employers`() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllEmployers(*someEmployers)
      val expectedEmployers = someEmployers.getEmployersResponse()

      val employersResponse = jobsBoardApiWebClient.getAllEmployers()

      with(expectedEmployers.content) {
        assertThat(employersResponse.content.size).isEqualTo(size)
        assertThat(employersResponse.content).containsOnly(*toTypedArray())
      }
    }

    @Test
    fun `return empty list, when no employer exist`() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllEmployers()

      val employersResponse = jobsBoardApiWebClient.getAllEmployers()

      assertThat(employersResponse.content).isEmpty()
      employersResponse.page.run {
        assertThat(number).isEqualTo(0)
        assertThat(totalElements).isEqualTo(0)
        assertThat(totalPages).isEqualTo(0)
      }
    }

    @Test
    fun `return a page of employers`() {
      hmppsAuth.stubGrantToken()
      val currentPage = 1
      val pageSize = 1
      val expectedTotalElements = someEmployers.size.toLong()
      val expectedEmployer = someEmployers[1]
      val expectedEmployerResponse = GetEmployerResponse.from(expectedEmployer)
      val expectedTotalPages = ceil(expectedTotalElements.toDouble() / pageSize).toInt()
      jobsBoardApi.stubRetrieveAllEmployers(currentPage, pageSize, expectedTotalElements, expectedEmployer)

      val employersResponse = jobsBoardApiWebClient.getAllEmployers(currentPage, pageSize)

      assertThat(employersResponse.content)
        .hasSize(1)
        .first().isEqualTo(expectedEmployerResponse)
      employersResponse.page.run {
        assertThat(number).isEqualTo(currentPage)
        assertThat(size).isEqualTo(pageSize)
        assertThat(totalPages).isEqualTo(expectedTotalPages)
        assertThat(totalElements).isEqualTo(expectedTotalElements)
      }
    }
  }

  @Nested
  @DisplayName("JobsBoard `GET` /jobs/{id}")
  inner class JobsGetDetailsEndpoint {
    @Test
    fun `return job details, given valid job ID`() {
      val job = tescoWarehouseHandler.makeCopy()

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveJob(job)

      val actualJob = jobsBoardApiWebClient.getJob(job.id)

      assertThat(actualJob).isEqualTo(job)
    }

    @Test
    fun `return nothing, given invalid job ID`() {
      val jobId = randomUUID()

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveJobNotFound()

      val actualJob = jobsBoardApiWebClient.getJob(jobId)

      assertThat(actualJob).isNull()
    }
  }

  @Nested
  @DisplayName("JobsBoard `GET` /jobs")
  inner class JobsRetrieveAllEndpoint {
    private val someJobs =
      arrayOf(tescoWarehouseHandler, amazonForkliftOperator, abcConstructionApprentice, nationalTescoWarehouseHandler).map { it.makeCopy() }.toTypedArray()

    @Test
    fun `return all jobs`() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllJobs(*someJobs)
      val expectedJobs = someJobs.getJobsResponse()

      val jobsResponse = jobsBoardApiWebClient.getAllJobs()

      with(expectedJobs.content) {
        assertThat(jobsResponse.content.size).isEqualTo(size)
        assertThat(jobsResponse.content).containsOnly(*toTypedArray())
      }
    }

    @Test
    fun `return empty list, when no job exist`() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllJobs()

      val jobsResponse = jobsBoardApiWebClient.getAllJobs()

      assertThat(jobsResponse.content).isEmpty()
      jobsResponse.page.run {
        assertThat(number).isEqualTo(0)
        assertThat(totalElements).isEqualTo(0)
        assertThat(totalPages).isEqualTo(0)
      }
    }

    @Test
    fun `return a page of jobs`() {
      hmppsAuth.stubGrantToken()
      val currentPage = 1
      val pageSize = 1
      val expectedTotalElements = someJobs.size.toLong()
      val expectedJob = someJobs[1]
      val expectedJobResponse = GetJobsData.from(expectedJob)
      val expectedTotalPages = ceil(expectedTotalElements.toDouble() / pageSize).toInt()
      jobsBoardApi.stubRetrieveAllJobs(currentPage, pageSize, expectedTotalElements, expectedJob)

      val employersResponse = jobsBoardApiWebClient.getAllJobs(currentPage, pageSize)

      assertThat(employersResponse.content)
        .hasSize(1)
        .first().isEqualTo(expectedJobResponse)
      employersResponse.page.run {
        assertThat(number).isEqualTo(currentPage)
        assertThat(size).isEqualTo(pageSize)
        assertThat(totalPages).isEqualTo(expectedTotalPages)
        assertThat(totalElements).isEqualTo(expectedTotalElements)
      }
    }
  }
}
