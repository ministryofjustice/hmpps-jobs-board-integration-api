package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.resource.integrationadmin

import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.application.ApplicationTestCase

abstract class ResendDataTestCase : ApplicationTestCase() {
  protected fun assertResendDataIsOk(endpoint: String, requestBody: String? = null) = webTestClient.put().uri(endpoint).also { put ->
    requestBody?.let { body -> put.contentType(MediaType.APPLICATION_JSON).bodyValue(body) }
  }.exchange().expectStatus().isOk

  protected fun assertResendDataIsExpected(
    endpoint: String,
    expectedItemCount: Long,
    expectedTotalCount: Long,
    requestBody: String? = null,
  ) = assertResendDataIsOk(endpoint, requestBody).expectBody()
    .jsonPath("$.itemCount").isEqualTo(expectedItemCount)
    .jsonPath("$.totalCount").isEqualTo(expectedTotalCount)

  protected fun makeRequestBody(ids: List<String>, idsFieldName: String) = ids.json().let {
    """
    {
      "$idsFieldName" : $it
    }
    """.trimIndent()
  }

  protected fun makeRequestBody(ids: List<String>, forceUpdate: Boolean, idsFieldName: String) = ids.json().let {
    """
    {
      "$idsFieldName" : $it,
      "forceUpdate" : $forceUpdate
    }
    """.trimIndent()
  }

  private fun List<String>.json() = map { "\"$it\"" }.joinToString(prefix = "[", postfix = "]")
}
