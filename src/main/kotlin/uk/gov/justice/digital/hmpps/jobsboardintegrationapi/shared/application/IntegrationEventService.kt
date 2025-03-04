package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventEmitter

class IntegrationEventService(
  val eventEmitter: EventEmitter,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleMessage(event: EventData) {
    log.info("handle message:  type=${event.eventType}, id=${event.eventId}")
    log.debug("handle message:  event={}", event)
    eventEmitter.send(event)
  }
}
