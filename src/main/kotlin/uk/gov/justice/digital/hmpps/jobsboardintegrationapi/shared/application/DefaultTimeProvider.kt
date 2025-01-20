package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.TimeProvider
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class DefaultTimeProvider(
  override val timezoneId: ZoneId = ZoneId.systemDefault(),
) : TimeProvider {
  override fun now(): LocalDateTime {
    return LocalDateTime.now()
  }
}
