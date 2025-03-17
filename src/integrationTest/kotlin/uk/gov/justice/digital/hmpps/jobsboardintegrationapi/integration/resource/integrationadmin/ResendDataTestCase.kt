package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.resource.integrationadmin

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.application.ApplicationTestCase
import kotlin.test.assertEquals

abstract class ResendDataTestCase : ApplicationTestCase() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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

  protected fun assertMessageQueuesAreEmpty() {
    log.debug("assertMessageQueuesAreEmpty|start")
    awaitIntegrationQueueAllMessagesAreGone(5)
    assertEquals(0, integrationDlqMessageCount())
    log.debug("assertMessageQueuesAreEmpty|end")
  }

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
