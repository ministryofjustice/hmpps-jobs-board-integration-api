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
  @Value("\${example-api.url}") val exampleApiBaseUri: String,
  @Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:20s}") val timeout: Duration,
  @Value("\${api.client.logging.enabled:false}") val clientLogging: Boolean,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  // TODO: Remove the health ping if no call outs to other services are made
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  // TODO: This is an example health bean for checking other services and should be removed / replaced
  @Bean
  fun exampleApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(exampleApiBaseUri, healthTimeout)

  // TODO: This is an example bean for calling other services and should be removed / replaced
  @Bean
  fun exampleApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    builder.authorisedWebClient(authorizedClientManager, registrationId = "example-api", url = exampleApiBaseUri, timeout)

  @ConditionalOnIntegrationEnabled
  @Bean
  fun jobsBoardWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${api.base.url.jobsboard}") jobsboardApiBaseUri: String,
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
