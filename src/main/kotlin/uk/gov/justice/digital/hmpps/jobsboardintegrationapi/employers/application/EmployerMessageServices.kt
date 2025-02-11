package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_CREATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_UPDATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class EmployerMessageService(
  employerEventTypes: Set<EmployerEventType>,
  protected val objectMapper: ObjectMapper,
) : IntegrationMessageService {
  constructor(employerEventType: EmployerEventType, objectMapper: ObjectMapper) : this(
    setOf(employerEventType),
    objectMapper,
  )

  protected val eventTypeTypes: Set<String> by lazy { employerEventTypes.map { it.type }.toSet() }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleMessage(integrationEvent: IntegrationEvent, messageAttributes: MessageAttributes) {
    log.info("handle message eventId=${integrationEvent.eventId}, eventType=${messageAttributes.eventType}, messageId=${messageAttributes.messageId}")
    messageAttributes.eventType?.also {
      log.trace("handle message. integrationEvent={}, messageAttributes={}", integrationEvent, messageAttributes)
      if (!eventTypeTypes.contains(it)) {
        throw IllegalArgumentException("Unexpected event type=$it , with eventId=${integrationEvent.eventId}, messageId=${messageAttributes.messageId}")
      }
      handleEvent(integrationEvent.toEmployerEvent())
    } ?: run {
      throw IllegalArgumentException("Missing event type eventId=${integrationEvent.eventId}, messageId=${messageAttributes.messageId}")
    }
  }

  protected abstract fun handleEvent(employerEvent: EmployerEvent)

  private fun IntegrationEvent.toEmployerEvent(): EmployerEvent = objectMapper.readValue(message, EmployerEvent::class.java)
}

class EmployerCreationMessageService(
  private val retriever: EmployerRetriever,
  private val registrar: EmployerRegistrar,
  objectMapper: ObjectMapper,
) : EmployerMessageService(EMPLOYER_CREATED, objectMapper) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleEvent(employerEvent: EmployerEvent) {
    log.info("handle employer creation event; eventId={}", employerEvent.eventId)
    try {
      retriever.retrieve(employerEvent.employerId).also { employer ->
        registrar.registerCreation(employer)
      }
    } catch (e: Exception) {
      throw Exception("Error at employer creation event: eventId=${employerEvent.eventId}", e)
    }
  }
}

class EmployerUpdateMessageService(
  private val retriever: EmployerRetriever,
  private val registrar: EmployerRegistrar,
  objectMapper: ObjectMapper,
) : EmployerMessageService(EMPLOYER_UPDATED, objectMapper) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleEvent(employerEvent: EmployerEvent) {
    log.info("handle employer update event; eventId={}", employerEvent.eventId)
    try {
      retriever.retrieve(employerEvent.employerId).also { employer ->
        registrar.registerUpdate(employer)
      }
    } catch (e: Exception) {
      throw Exception("Error at employer update event: eventId=${employerEvent.eventId}", e)
    }
  }
}
