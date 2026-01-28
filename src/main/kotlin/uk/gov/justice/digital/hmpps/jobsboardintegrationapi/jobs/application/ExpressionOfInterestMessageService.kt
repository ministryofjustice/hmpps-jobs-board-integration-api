package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.HmppsMessageServiceBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType.EXPRESSION_OF_INTEREST_CREATED

class ExpressionOfInterestMessageService(
  private val expressionOfInterestRegistrar: ExpressionOfInterestRegistrar,
  jsonMapper: JsonMapper,
) : HmppsMessageServiceBase(EXPRESSION_OF_INTEREST_CREATED, jsonMapper) {

  override fun handleHmppsMessage(hmppsMessage: HmppsMessage, eventType: HmppsMessageEventType) {
    assert(eventType == EXPRESSION_OF_INTEREST_CREATED) { "Unsupported eventType: $eventType" }
    val jobExternalId = requireNotNull(hmppsMessage.messageAttributes["jobId"]) { "Missing jobId" }.let {
      it.toLongOrNull() ?: run { throw IllegalArgumentException("Invalid jobId=$it") }
    }
    val prisonNumber = requireNotNull(hmppsMessage.messageAttributes["prisonNumber"]) { "Missing prisonNumber" }
    expressionOfInterestRegistrar.registerCreation(jobExternalId, prisonNumber)
  }
}
