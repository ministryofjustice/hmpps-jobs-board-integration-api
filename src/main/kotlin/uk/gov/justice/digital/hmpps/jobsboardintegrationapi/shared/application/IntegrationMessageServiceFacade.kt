package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

@Service
@Qualifier("integrationMessageService")
class IntegrationMessageServiceFacade(
  @Qualifier("integrationServiceMap") private val serviceMap: Map<String, IntegrationMessageService>,
) : IntegrationMessageService {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleMessage(integrationEvent: IntegrationEvent, messageAttributes: MessageAttributes) {
    val eventId = integrationEvent.eventId
    val eventType = messageAttributes.eventType
    log.info("received event: id=$eventId, type=$eventType")
    log.trace("received event: {}, {}", integrationEvent, messageAttributes)

    messageAttributes.eventType?.also { eventTypeAttr ->
      serviceMap[eventTypeAttr]?.also { service ->
        service.handleMessage(integrationEvent, messageAttributes)
      } ?: run {
        logAndThrowArgumentError("MessageService not found for Event type=$eventType, eventId=$eventId")
      }
    } ?: run {
      logAndThrowArgumentError("Missing event type eventId=$eventId")
    }
  }

  private fun logAndThrowArgumentError(message: String) {
    log.error(message)
    throw IllegalArgumentException(message)
  }
}
