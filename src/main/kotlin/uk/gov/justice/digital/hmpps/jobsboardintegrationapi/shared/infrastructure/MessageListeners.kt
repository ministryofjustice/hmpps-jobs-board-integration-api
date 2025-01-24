package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService

const val ATTRIBUTE_EVENT_TYPE = "eventType"
const val ATTRIBUTE_EVENT_ID = "eventId"
const val ATTRIBUTE_ID = "id"

class IntegrationMessageListener(
  private val integrationMessageService: IntegrationMessageService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @MessageMapping
  @SqsListener("integrationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(
    message: String,
    @Header(ATTRIBUTE_EVENT_TYPE) eventType: String?,
    @Header(ATTRIBUTE_EVENT_ID) eventId: String?,
    @Header(ATTRIBUTE_ID) messageId: String?,
  ) {
    log.info("processMessage()|Processing message:messageId=$messageId, eventType=$eventType, eventId=$eventId")
    val messageAttributes = MessageAttributes().also { attributes ->
      mapOf(
        ATTRIBUTE_EVENT_TYPE to eventType,
        ATTRIBUTE_EVENT_ID to eventId,
        ATTRIBUTE_ID to messageId,
      ).filterValues { it != null }.let { attributes.putAll(it) }
    }
    val event = IntegrationEvent(
      eventType = eventType,
      message = message,
    )
    integrationMessageService.handleMessage(event, messageAttributes)
  }
}

class MessageAttributes() : HashMap<String, Any?>() {
  constructor(eventType: String) : this() {
    put(ATTRIBUTE_EVENT_TYPE, eventType)
  }

  constructor(source: Map<String, Any?>) : this() {
    putAll(source)
  }

  val eventType: String? get() = get(ATTRIBUTE_EVENT_TYPE) as? String?
  val eventId: String? get() = get(ATTRIBUTE_EVENT_ID) as? String?
  val messageId: String? get() = get(ATTRIBUTE_ID) as? String?
}
