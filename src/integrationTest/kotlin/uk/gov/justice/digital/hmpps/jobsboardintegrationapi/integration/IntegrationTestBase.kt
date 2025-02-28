package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.testcontainers.PostgresContainer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension.Companion.jobsBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MNJobBoardApiExtension
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.DefaultTimeProvider
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@ExtendWith(
  HmppsAuthApiExtension::class,
  JobsBoardApiExtension::class,
  MNJobBoardApiExtension::class,
  MockitoExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    private val postgresContainer = PostgresContainer.flywayContainer

    @JvmStatic
    @DynamicPropertySource
    fun configureTestContainers(registry: DynamicPropertyRegistry) {
      postgresContainer?.run {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
        registry.add("spring.datasource.username", postgresContainer::getUsername)
        registry.add("spring.datasource.password", postgresContainer::getPassword)
        registry.add("spring.flyway.url", postgresContainer::getJdbcUrl)
        registry.add("spring.flyway.user", postgresContainer::getUsername)
        registry.add("spring.flyway.password", postgresContainer::getPassword)
      }

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
    jobsBoardApi.stubHealthPing(status)
  }

  protected fun randomUUID() = UUID.randomUUID().toString()
}
