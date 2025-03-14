package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType.JOB_CREATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType.JOB_UPDATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class JobMessageService(
  jobEventTypes: Set<JobEventType>,
  protected val objectMapper: ObjectMapper,
) : IntegrationMessageService {
  constructor(jobEventType: JobEventType, objectMapper: ObjectMapper) : this(
    setOf(jobEventType),
    objectMapper,
  )

  protected val eventTypeTypes: Set<String> by lazy { jobEventTypes.map { it.type }.toSet() }

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
      handleEvent(integrationEvent.toJobEvent())
    } ?: run {
      throw IllegalArgumentException("Missing event type eventId=${integrationEvent.eventId}, messageId=${messageAttributes.messageId}")
    }
  }

  protected abstract fun handleEvent(jobEvent: JobEvent)

  private fun IntegrationEvent.toJobEvent(): JobEvent = objectMapper.readValue(message, JobEvent::class.java)
}

class JobCreationMessageService(
  private val retriever: JobRetriever,
  private val registrar: JobRegistrar,
  objectMapper: ObjectMapper,
) : JobMessageService(JOB_CREATED, objectMapper) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleEvent(jobEvent: JobEvent) {
    log.info("handle job creation event; eventId={}", jobEvent.eventId)
    try {
      retriever.retrieve(jobEvent.jobId).also { job ->
        registrar.registerCreation(job)
      }
    } catch (e: Exception) {
      throw Exception("Error at job creation event: eventId=${jobEvent.eventId}", e)
    }
  }
}

class JobUpdateMessageService(
  private val retriever: JobRetriever,
  private val registrar: JobRegistrar,
  objectMapper: ObjectMapper,
) : JobMessageService(JOB_UPDATED, objectMapper) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleEvent(jobEvent: JobEvent) {
    log.info("handle job update event; eventId={}", jobEvent.eventId)
    try {
      retriever.retrieve(jobEvent.jobId).also { job ->
        registrar.registerUpdate(job)
      }
    } catch (e: Exception) {
      throw Exception("Error at job update event: eventId=${jobEvent.eventId}", e)
    }
  }
}
