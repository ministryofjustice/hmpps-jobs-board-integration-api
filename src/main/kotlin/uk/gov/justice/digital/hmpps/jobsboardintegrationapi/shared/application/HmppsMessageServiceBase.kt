package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class HmppsMessageServiceBase(
  protected val eventTypes: Set<HmppsMessageEventType>,
  protected val objectMapper: ObjectMapper,
) : IntegrationMessageService {
  constructor(eventType: HmppsMessageEventType, objectMapper: ObjectMapper) : this(
    setOf(eventType),
    objectMapper,
  )

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleMessage(integrationEvent: IntegrationEvent, messageAttributes: MessageAttributes) {
    log.info("handle message eventId=${integrationEvent.eventId}, eventType=${messageAttributes.eventType}, messageId=${messageAttributes.messageId}")

    try {
      messageAttributes.eventType?.also {
        log.trace("handle message. integrationEvent={}, messageAttributes={}", integrationEvent, messageAttributes)
        val eventType = requireNotNull(HmppsMessageEventType.fromType(it)) { "Unknown event type: $it" }
        if (!eventTypes.contains(eventType)) {
          throw IllegalArgumentException("Unexpected event type: $it")
        }
        handleHmppsMessage(integrationEvent.hmppsMessage(), eventType)
      } ?: run {
        throw IllegalArgumentException("Missing event type!")
      }
    } catch (ex: Exception) {
      "${ex.message}, eventId=${integrationEvent.eventId}, messageId=${messageAttributes.messageId}"
        .let { throw Exception(it, ex) }
    }
  }

  protected abstract fun handleHmppsMessage(hmppsMessage: HmppsMessage, eventType: HmppsMessageEventType)

  private fun IntegrationEvent.hmppsMessage(): HmppsMessage = objectMapper.readValue(message, HmppsMessage::class.java)
}
