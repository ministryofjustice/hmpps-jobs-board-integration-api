package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
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
  protected lateinit var uuidGenerator: UUIDGenerator

  @Mock
  protected lateinit var mockedObjectMapper: ObjectMapper

  protected val objectMapper: JsonMapper by lazy {
    JsonMapper.builder()
      .addModule(KotlinModule.Builder().build())
      .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .build()
  }

  protected val defaultTimeZoneOffset = ZoneOffset.UTC!!
  protected val defaultTimeZone: ZoneId = defaultTimeZoneOffset
  protected val defaultCurrentLocalTime = LocalDateTime.of(2025, 1, 1, 1, 1, 1)!!
  protected val defaultCurrentTime: Instant by lazy { defaultCurrentLocalTime.atZone(defaultTimeZone).toInstant() }

  @BeforeEach
  internal open fun setUpBase() {
    lenient().whenever(uuidGenerator.generate()).thenReturn(UUID.randomUUID().toString())
    lenient().whenever(timeProvider.nowAsInstant()).thenReturn(defaultCurrentTime)
    lenient().whenever(timeProvider.now()).thenReturn(defaultCurrentLocalTime)
  }

  protected fun randomUUID() = UUID.randomUUID().toString()
}
