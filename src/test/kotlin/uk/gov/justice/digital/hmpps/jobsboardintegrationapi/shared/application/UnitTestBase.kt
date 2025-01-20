package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.TimeProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockitoExtension::class)
abstract class UnitTestBase {

  @Mock
  protected lateinit var timeProvider: TimeProvider

  @Mock
  protected lateinit var mockedObjectMapper: ObjectMapper

  protected val objectMapper by lazy {
    jacksonObjectMapper().also {
      it.registerModule(JavaTimeModule())
      it.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
  }

  protected val defaultTimeZoneOffset = ZoneOffset.UTC
  protected val defaultTimeZone: ZoneId = defaultTimeZoneOffset
  protected val defaultCurrentLocalTime = LocalDateTime.of(2025, 1, 1, 1, 1, 1)
  protected val defaultCurrentTime: Instant by lazy { defaultCurrentLocalTime.atZone(defaultTimeZone).toInstant() }

  protected fun randomUUID() = UUID.randomUUID().toString()
}
