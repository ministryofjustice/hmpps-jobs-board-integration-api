package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

interface IntegrationMessageService {
  fun handleMessage(integrationEvent: IntegrationEvent, messageAttributes: MessageAttributes)
}

data class IntegrationEvent(
  val eventId: String? = null,
  val eventType: String? = null,
  val message: String? = null,
)
