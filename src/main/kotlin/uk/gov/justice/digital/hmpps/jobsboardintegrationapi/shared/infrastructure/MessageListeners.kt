package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService

const val ATTRIBUTE_EVENT_TYPE = "eventType"

class IntegrationMessageListener(
  private val integrationMessageService: IntegrationMessageService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("integrationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(message: Message) {
    val event = objectMapper.readValue(message.message, IntegrationEvent::class.java)
    integrationMessageService.handleMessage(event, message.messageAttributes)
  }
}

data class MessageAttribute(val value: String, val type: String) {
  constructor(value: String) : this(value, "String")
}
typealias EventType = MessageAttribute

class MessageAttributes() : HashMap<String, MessageAttribute>() {
  constructor(attribute: EventType) : this() {
    put(ATTRIBUTE_EVENT_TYPE, attribute)
  }

  val eventType: String? get() = this[ATTRIBUTE_EVENT_TYPE]?.value
}

data class Message(
  val message: String,
  val messageId: String,
  val messageAttributes: MessageAttributes,
)
