package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import java.time.Instant

interface EventEmitter {
  fun send(event: EventData)
}

data class EventData(
  val eventId: String,
  val eventType: String,
  val timestamp: Instant,
  val content: String,
)
