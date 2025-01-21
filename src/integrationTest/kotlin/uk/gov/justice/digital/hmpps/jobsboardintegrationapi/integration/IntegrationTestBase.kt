package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.testcontainers.LocalStackContainer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.testcontainers.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.ExampleApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.ExampleApiExtension.Companion.exampleApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.DefaultTimeProvider
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@ExtendWith(
  HmppsAuthApiExtension::class,
  ExampleApiExtension::class,
  JobsBoardApiExtension::class,
  MockitoExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @MockitoSpyBean
  protected lateinit var timeProvider: DefaultTimeProvider

  val defaultTimezoneId = ZoneId.of("Z")
  val defaultCurrentTime: Instant = Instant.parse("2024-01-01T00:00:00Z")
  val defaultCurrentTimeLocal: LocalDateTime get() = defaultCurrentTime.atZone(defaultTimezoneId).toLocalDateTime()

  companion object {
    private val localStackContainer by lazy { LocalStackContainer.instance }

    @JvmStatic
    @DynamicPropertySource
    fun configureTestContainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @BeforeEach
  internal fun setUp() {
    whenever(timeProvider.timezoneId).thenReturn(defaultTimezoneId)
    whenever(timeProvider.now()).thenReturn(defaultCurrentTimeLocal)
  }

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    exampleApi.stubHealthPing(status)
  }

  protected fun randomUUID() = UUID.randomUUID().toString()
}
