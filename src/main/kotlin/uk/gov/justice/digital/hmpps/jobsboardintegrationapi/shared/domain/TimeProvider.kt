package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface TimeProvider {
  val timezoneId: ZoneId

  fun now(): LocalDateTime

  fun nowAsInstant(): Instant = now().atZone(timezoneId).toInstant()
}
