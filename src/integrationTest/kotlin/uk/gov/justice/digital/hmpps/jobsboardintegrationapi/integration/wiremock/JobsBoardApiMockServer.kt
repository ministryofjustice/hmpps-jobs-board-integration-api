package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiMockServer.Companion.mapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobsData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobsResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.PageResponse

class JobsBoardApiMockServer : WireMockServer(8092) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  // stubbing retrieve employer
  fun stubRetrieveEmployer(employer: Employer) {
    stubFor(
      get(urlPathTemplate(EMPLOYER_PATH_TEMPLATE))
        .withHeader("Authorization", containing("Bearer"))
        .withPathParam(PATH_ID, equalTo(employer.id))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(employer.response()),
        ),
    )
  }

  fun stubRetrieveEmployer(employers: List<Employer>) = employers.forEach { stubRetrieveEmployer(it) }

  fun stubRetrieveEmployerNotFound() {
    stubFor(
      get(urlPathMatching(EMPLOYER_PATH_REGEX))
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )
  }

  // stubbing retrieve all employers
  fun stubRetrieveAllEmployers(employers: List<Employer>) = stubRetrieveAllEmployers(employers.getEmployersResponse())
  fun stubRetrieveAllEmployers(vararg employer: Employer) = stubRetrieveAllEmployers(employer.getEmployersResponse())

  fun stubRetrieveAllEmployers(page: Int, pageSize: Int, totalElements: Long, vararg employer: Employer) = employer.map { GetEmployerResponse.from(it) }.toTypedArray()
    .let { stubRetrieveAllEmployers(GetEmployersResponse.from(number = page, size = pageSize, totalElements = totalElements, items = it)) }

  fun stubRetrieveAllEmployers(getEmployersResponse: GetEmployersResponse) {
    stubFor(
      get(urlPathMatching(EMPLOYERS_PATH_REGEX))
        .withHeader("Authorization", containing("Bearer"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(getEmployersResponse.employersResponse()),
        ),
    )
  }

  // stubbing retrieve job
  fun stubRetrieveJob(job: Job) {
    stubFor(
      get(urlPathTemplate(JOB_PATH_TEMPLATE))
        .withHeader("Authorization", containing("Bearer"))
        .withPathParam(PATH_ID, equalTo(job.id))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(job.response()),
        ),
    )
  }

  fun stubRetrieveJobNotFound() {
    stubFor(
      get(urlPathMatching(JOB_PATH_REGEX))
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )
  }

  // stubbing retrieve all jobs
  fun stubRetrieveAllJobs(vararg job: Job) = stubRetrieveAllJobs(job.getJobsResponse())

  fun stubRetrieveAllJobs(page: Int, pageSize: Int, totalElements: Long, vararg job: Job) = job.map { GetJobsData.from(it) }.toTypedArray()
    .let { stubRetrieveAllJobs(GetJobsResponse.from(number = page, size = pageSize, totalElements = totalElements, items = it)) }

  fun stubRetrieveAllJobs(getJobsResponse: GetJobsResponse) {
    stubFor(
      get(urlPathMatching(JOBS_PATH_REGEX))
        .withHeader("Authorization", containing("Bearer"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(getJobsResponse.jobsResponse()),
        ),
    )
  }

  companion object {
    private const val PATH_ID = "id"
    private const val EMPLOYER_PATH_TEMPLATE = "/employers/{$PATH_ID}"
    private const val EMPLOYER_PATH_REGEX = "/employers/[a-zA-Z0-9\\-]*"
    private const val EMPLOYERS_PATH_REGEX = "/employers[?.+]*"

    private const val JOB_PATH_TEMPLATE = "/jobs/{$PATH_ID}"
    private const val JOB_PATH_REGEX = "/jobs/[a-zA-Z0-9\\-]*"
    private const val JOBS_PATH_REGEX = "/jobs[?.+]*"

    internal val mapper: ObjectMapper = jacksonObjectMapper()
  }
}

class JobsBoardApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val jobsBoardApi = JobsBoardApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = jobsBoardApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = jobsBoardApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = jobsBoardApi.stop()
}

fun List<Employer>.getEmployersResponse() = GetEmployersResponse.from(*map { GetEmployerResponse.from(it) }.toTypedArray())

fun Array<out Employer>.getEmployersResponse() = GetEmployersResponse.from(*this.map { GetEmployerResponse.from(it) }.toTypedArray())

fun List<Job>.getJobsResponse() = GetJobsResponse.from(*map { GetJobsData.from(it) }.toTypedArray())
fun Array<out Job>.getJobsResponse() = GetJobsResponse.from(*this.map { GetJobsData.from(it) }.toTypedArray())

private fun Employer.response() = GetEmployerResponse.from(this).response()

private fun GetEmployerResponse.response() = """
  {
    "id": "$id",
    "name": "$name",
    "description": "$description",
    "sector": "$sector",
    "status": "$status",
    "createdAt": "$createdAt"
  }
""".trimIndent()

private fun GetEmployersResponse.employersResponse() = let { response ->
  response.content.map { it.response() }.let { PageResponse(it, response.page) }.response()
}
private fun GetJobsResponse.jobsResponse() = let { response ->
  response.content.map { it.responseItem() }.let { PageResponse(it, response.page) }.response()
}

private fun PageResponse<String>.response() = if (content.isNotEmpty()) {
  content.map { "\n${it.prependIndent(" ".repeat(8))}" }.joinToString(postfix = "\n${" ".repeat(4)}")
} else {
  ""
}.let { contentString ->
  """
    {
      "content": [$contentString],
      "page": {
        "size": ${page.size},
        "number": ${page.number},
        "totalElements": ${page.totalElements},
        "totalPages": ${page.totalPages}
      }
    }
  """.trimIndent()
}

private fun Job.response() = GetJobResponse.from(this).response().also { println("job.response()=$it") }

private fun GetJobResponse.response() = """
  {
    "id": "$id",
    "employerId": "$employerId",
    "jobTitle": "$jobTitle",
    "sector": "$sector",
    "industrySector": "$industrySector",
    "numberOfVacancies": $numberOfVacancies,
    "sourcePrimary": "$sourcePrimary",
    "sourceSecondary": ${sourceSecondary?.asJson()},
    "charityName": ${charityName?.asJson()},
    "postCode": "$postCode",
    "salaryFrom": $salaryFrom,
    "salaryTo": $salaryTo,
    "salaryPeriod": "$salaryPeriod",
    "additionalSalaryInformation": ${additionalSalaryInformation?.asJson()},
    "isPayingAtLeastNationalMinimumWage": $isPayingAtLeastNationalMinimumWage,
    "workPattern": "$workPattern",
    "hoursPerWeek": "$hoursPerWeek",
    "contractType": "$contractType",
    "baseLocation": ${baseLocation?.asJson()},
    "essentialCriteria": "$essentialCriteria",
    "desirableCriteria": ${desirableCriteria?.asJson()},
    "description": ${description.asJson()},
    "offenceExclusions": ${offenceExclusions.asStringList()},
    "offenceExclusionsDetails": ${offenceExclusionsDetails?.asJson()}, 
    "isRollingOpportunity": $isRollingOpportunity,
    "closingDate": ${closingDate?.asJson()},
    "isOnlyForPrisonLeavers": $isOnlyForPrisonLeavers,
    "startDate": ${startDate?.asJson()},
    "howToApply": "$howToApply",
    "supportingDocumentationRequired": ${supportingDocumentationRequired?.asStringList()},
    "supportingDocumentationDetails": ${supportingDocumentationDetails?.asJson()},
    "createdAt": "$createdAt"
  }
""".trimIndent()

private fun GetJobsData.responseItem() = """
  {
    "id": "$id",
    "employerId": "$employerId",
    "employerName": "$employerName",
    "jobTitle": "$jobTitle",
    "numberOfVacancies": $numberOfVacancies,
    "sector": "$sector",
    "createdAt": "$createdAt"
  }
""".trimIndent()

private fun String.asJson(): String = mapper.writeValueAsString(this)
private fun List<String>.asStringList() = joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
