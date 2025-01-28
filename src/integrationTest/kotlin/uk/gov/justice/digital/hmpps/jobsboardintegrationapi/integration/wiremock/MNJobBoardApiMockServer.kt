package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import java.util.concurrent.atomic.AtomicLong

private const val EMPLOYERS_ENDPOINT = "/employers"

class MNJobBoardApiMockServer(
  private val nextId: AtomicLong = AtomicLong(1),
) : WireMockServer(8093) {
  fun stubCreateEmployer(mnEmployer: MNEmployer) = stubCreateEmployer(mnEmployer, nextId.getAndIncrement())
  fun stubCreateEmployer(mnEmployer: MNEmployer, newId: Long) {
    val employerCreated = mnEmployer.copy(id = newId)
    stubFor(
      post(EMPLOYERS_ENDPOINT)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(employerCreated.response()),
        ),
    )
  }

  fun stubCreateEmployerUnauthorised() {
    stubFor(
      post(EMPLOYERS_ENDPOINT)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withStatus(401),
        ),
    )
  }
}

class MNJobBoardApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val mnJobBoardApi = MNJobBoardApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = mnJobBoardApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = mnJobBoardApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = mnJobBoardApi.stop()
}

private fun MNEmployer.response() = """
  {
    "message": {
        "successCode": "J2047",
        "successMessage": "Successfully added employer",
        "httpStatusCode": 201
    },
    "responseObject": {
        "id": $id,
        "employerName": "$employerName",
        "employerBio": "$employerBio",
        "sectorId": $sectorId,
        "partnerId": $partnerId,
        "imgName": ${imgName?.let { "\"$it\"" }},
        "path": ${path?.let { "\"$it\"" }}
    }
  }
""".trimIndent()
