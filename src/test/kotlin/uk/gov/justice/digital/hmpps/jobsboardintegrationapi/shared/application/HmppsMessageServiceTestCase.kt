package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class HmppsMessageServiceTestCase : UnitTestBase() {

  protected fun makeMessageAttributes(eventType: HmppsMessageEventType) = makeMessageAttributes(eventType.type)

  protected fun makeMessageAttributes(eventType: String) = mapOf("eventType" to eventType)

  protected fun HmppsMessage.integrationEvent(eventTypeAttribute: String? = null) = IntegrationEvent(
    eventId = messageId,
    eventType = eventTypeAttribute ?: eventType.type,
    message = objectMapper.writeValueAsString(this),
  )

  protected fun HmppsMessage.integrationMessageAttributes() = this.messageAttributes["eventType"]
    ?.let { MessageAttributes(it) } ?: MessageAttributes()
}
