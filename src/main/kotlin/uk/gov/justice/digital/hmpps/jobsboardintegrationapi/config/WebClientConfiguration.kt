package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import io.netty.handler.logging.LogLevel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.base.url.jobsboard}") val jobsboardApiBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
  @param:Value("\${api.client.logging.enabled:false}") val clientLogging: Boolean,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun jobsBoardHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(jobsboardApiBaseUri, healthTimeout)

  @ConditionalOnIntegrationEnabled
  @Bean
  fun jobsBoardWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.apply {
    if (clientLogging) it.clientConnector(clientConnectorWithLogging())
  }.authorisedWebClient(
    authorizedClientManager,
    registrationId = "hmpps-jobs-board-api",
    url = jobsboardApiBaseUri,
    timeout,
  )

  @ConditionalOnIntegrationEnabled
  @Bean
  fun mnJobBoardWebClient(
    builder: WebClient.Builder,
    @Value("\${api.base.url.mnjobboard}") mnjobboardApiBaseUri: String,
    @Value("\${mn.jobboard.api.token}") mnJobBoardToken: String,
  ): WebClient = builder.defaultHeader("Authorization", "Bearer $mnJobBoardToken").baseUrl(mnjobboardApiBaseUri).apply {
    if (clientLogging) it.clientConnector(clientConnectorWithLogging())
  }.build()

  private fun clientConnectorWithLogging() = ReactorClientHttpConnector(httpClientWireTapLogging())

  private fun httpClientWireTapLogging() = HttpClient.create().wiretap(
    "reactor.netty.http.client.HttpClient",
    LogLevel.DEBUG,
    AdvancedByteBufFormat.TEXTUAL,
  )
}
