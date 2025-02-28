package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

class JobsBoardApiMockServer : WireMockServer(8092) {
  private val retrieveEmployerPathRegex = "/employers/[a-zA-Z0-9\\-]*"

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
      get(urlPathMatching(retrieveEmployerPathRegex))
        .withHeader("Authorization", containing("Bearer"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(employer.response()),
        ),
    )
  }

  fun stubRetrieveEmployerNotFound() {
    stubFor(
      get(urlPathMatching(retrieveEmployerPathRegex))
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )
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

private fun Employer.response() = """
  {
    "id": "$id",
    "name": "$name",
    "description": "$description",
    "sector": "$sector",
    "status": "$status",
    "createdAt": ${createdAt?.let { "\"$it\"" }}
  }
""".trimIndent()
