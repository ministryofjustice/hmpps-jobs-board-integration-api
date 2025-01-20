package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes
import java.time.Instant

interface IntegrationMessageService {
  fun handleMessage(integrationEvent: IntegrationEvent, messageAttributes: MessageAttributes)
}

data class IntegrationEvent(
  val eventId: String,
  val eventType: String,
  val timestamp: Instant,
  val content: String,
)
