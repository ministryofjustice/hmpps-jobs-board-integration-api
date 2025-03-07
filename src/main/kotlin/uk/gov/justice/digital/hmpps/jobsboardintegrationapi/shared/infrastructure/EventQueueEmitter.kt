package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventEmitter
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue as SqsMessageAttributeValue

class EventQueueEmitter(
  private val hmppsQueueService: HmppsQueueService,
) : EventEmitter {
  private val queueId: String = INTEGRATION_QUEUE_ID

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val integrationQueue by lazy {
    log.info("Initialise integrationQueue")
    hmppsQueueService.findByQueueId(queueId) ?: throw MissingQueueException("Could not find queue $queueId")
  }

  fun forceInit() {
    log.debug("Force initialise earlier")
    integrationQueue
  }

  override fun send(event: EventData) {
    integrationQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(integrationQueue.queueUrl)
        .messageBody(event.content)
        .messageAttributes(event.messageAttributes())
        .build()
        .also { log.trace("Send event {} to integration queue", event) },
    ).get().let {
      log.info("Sent event ${event.eventId} to integration queue")
    }
  }

  private fun EventData.messageAttributes() = eventAttributesToMessageAttributes(eventType, eventId)

  private fun eventAttributesToMessageAttributes(
    eventType: String,
    eventId: String? = null,
    noTracing: Boolean = false,
  ): Map<String, SqsMessageAttributeValue> = buildMap {
    put("eventType", attributeValue(eventType))
    eventId?.let { put("eventId", attributeValue(it)) }
    if (noTracing) put("noTracing", attributeValue("true"))
  }

  private fun attributeValue(value: String) = SqsMessageAttributeValue.builder().dataType("String").stringValue(value).build()
}
