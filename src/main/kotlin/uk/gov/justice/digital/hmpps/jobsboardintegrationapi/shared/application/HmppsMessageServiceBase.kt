package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class HmppsMessageServiceBase(
  protected val eventTypes: Set<HmppsMessageEventType>,
  protected val jsonMapper: JsonMapper,
) : IntegrationMessageService {
  constructor(eventType: HmppsMessageEventType, jsonMapper: JsonMapper) : this(
    setOf(eventType),
    jsonMapper,
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

  private fun IntegrationEvent.hmppsMessage(): HmppsMessage = jsonMapper.readValue(message, HmppsMessage::class.java)
}
