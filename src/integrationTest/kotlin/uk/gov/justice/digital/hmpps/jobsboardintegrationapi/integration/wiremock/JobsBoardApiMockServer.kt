package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse

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

  fun stubRetrieveEmployer(employer: Employer) {
    stubFor(
      get(urlPathTemplate(EMPLOYER_PATH_TEMPLATE))
        .withHeader("Authorization", containing("Bearer"))
        .withPathParam(EMPLOYER_PATH_ID, equalTo(employer.id))
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
            .withBody(getEmployersResponse.response()),
        ),
    )
  }

  companion object {
    private const val EMPLOYER_PATH_ID = "id"
    private const val EMPLOYER_PATH_TEMPLATE = "/employers/{$EMPLOYER_PATH_ID}"
    private const val EMPLOYER_PATH_REGEX = "/employers/[a-zA-Z0-9\\-]*"
    private const val EMPLOYERS_PATH_REGEX = "/employers[?.+]*"
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

private fun GetEmployersResponse.response() = if (content.isNotEmpty()) {
  content.map { "\n${it.response().prependIndent(" ".repeat(8))}" }.joinToString(postfix = "\n${" ".repeat(4)}")
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
