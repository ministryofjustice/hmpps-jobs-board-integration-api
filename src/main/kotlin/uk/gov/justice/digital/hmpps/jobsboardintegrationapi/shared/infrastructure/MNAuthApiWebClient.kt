package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.logging.LogLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.MediaType
import org.springframework.http.client.ReactorClientHttpRequestFactory
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.GenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNApiError
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNAuthApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNAuthClientManager
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNAuthGrantRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNAuthToken
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*

class MNAuthAuthorizedClientManager(
  clientRegistrationRepository: ClientRegistrationRepository,
  mnAuthApiClient: MNAuthApiClient,
) : MNAuthClientManager {
  private val authorisedClientManager: OAuth2AuthorizedClientManager
  private val authorisedClientService: OAuth2AuthorizedClientService

  init {
    authorisedClientService = InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
    authorisedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorisedClientService)
      .apply {
        setAuthorizedClientProvider(ClientCredentialsOAuth2AuthorizedClientProvider().apply { setAccessTokenResponseClient(mnAuthApiClient) })
      }
  }

  override fun authorize(authorizeRequest: OAuth2AuthorizeRequest?): OAuth2AuthorizedClient? = authorisedClientManager.authorize(authorizeRequest)
}

class MNAuthOAuth2AccessTokenResponseClient(
  private val objectMapper: ObjectMapper,
  private val appId: Long,
  private val clientLogging: Boolean = false,
  private val timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
) : MNAuthApiClient {
  private val tokenParser = TokenParser()
  private var headersConverter: Converter<OAuth2ClientCredentialsGrantRequest, HttpHeaders>
  private var parametersConverter: Converter<OAuth2ClientCredentialsGrantRequest, MNAuthGrantRequest>
  private var jsonMessageConverter: GenericHttpMessageConverter<Any?>
  private var responseConverter: Converter<HttpHeaders, OAuth2AccessTokenResponse>

  private var restClient: RestClient

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    headersConverter = mnHeadersConverter()
    parametersConverter = mnParametersConverter()
    jsonMessageConverter = MappingJackson2HttpMessageConverter()
    responseConverter = mnAuthResponseConverter()

    restClient = mnRestClient()
  }

  override fun getTokenResponse(grantRequest: OAuth2ClientCredentialsGrantRequest): OAuth2AccessTokenResponse {
    try {
      log.debug("Retrieving access token")
      return this.restClient.post().uri(grantRequest.getClientRegistration().getProviderDetails().getTokenUri())
        .headers({ h -> h.putAll(this.headersConverter.convert(grantRequest)!!) })
        .body(this.parametersConverter.convert(grantRequest)!!)
        .retrieve()
        .toBodilessEntity()
        .run { responseConverter.convert(headers) }!!
        .also { log.debug("Retrieved access token successfully! token's expiresAt is {}", it.accessToken.expiresAt) }
    } catch (ex: RestClientException) {
      log.error("Failed to retrieve access token: {}", ex.message)
      throw authException(ex)
    }
  }

  private fun mnRestClient(): RestClient {
    val requestFactory = (if (!clientLogging) ReactorClientHttpRequestFactory() else ReactorClientHttpRequestFactory(httpClientWireTapLogging()))
      .apply { setReadTimeout(timeout) }

    return RestClient.builder()
      .messageConverters { messageConverters: MutableList<HttpMessageConverter<*>?> ->
        messageConverters.clear()
        messageConverters.add(FormHttpMessageConverter())
        messageConverters.add(jsonMessageConverter)
      }
      .defaultStatusHandler(OAuth2ErrorResponseErrorHandler().apply { setErrorConverter(MNAuthErrorResponseConverter()) })
      .requestFactory(requestFactory).build()
  }

  private fun mnHeadersConverter() = Converter<OAuth2ClientCredentialsGrantRequest, HttpHeaders> { grantRequest ->
    HttpHeaders().apply {
      contentType = APPLICATION_JSON_UTF8
      accept = ACCEPT_ALL
    }
  }

  private fun mnParametersConverter() = Converter<OAuth2ClientCredentialsGrantRequest, MNAuthGrantRequest> { grantRequest ->
    grantRequest.clientRegistration.let {
      MNAuthGrantRequest(
        appId = appId,
        userName = it.clientId,
        password = it.clientSecret,
      )
    }
  }

  private fun mnAuthResponseConverter() = Converter<HttpHeaders, OAuth2AccessTokenResponse> { headers ->
    headers.let { it.getFirst(it.accessControlExposeHeaders.first()) }
      .ifEmpty { throw IllegalArgumentException("Bearer Token is missing") }
      .let { bearerToken -> tokenParser.parseAccessToken(bearerToken) }
      .let { accessToken ->
        accessToken.run {
          OAuth2AccessTokenResponse
            .withToken(tokenValue)
            .tokenType(tokenType)
            .expiresIn(Duration.between(Instant.now(), expiresAt).seconds)
            .build()
        }
      }
  }

  private fun authException(ex: Throwable) = OAuth2Error(
    OAUTH2_ERROR_CODE,
    "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: ${ex.message}",
    null,
  ).let { OAuth2AuthorizationException(it, ex) }

  private inner class MNAuthErrorResponseConverter : OAuth2ErrorHttpMessageConverter() {
    override fun readInternal(clazz: Class<out OAuth2Error?>, inputMessage: HttpInputMessage): OAuth2Error = (jsonMessageConverter.read(MNApiError::class.java, inputMessage) as MNApiError)
      .run { OAuth2Error(errorCode, "Error with MN-Auth: $this", null) }
  }

  private inner class TokenParser {
    private val decoder = Base64.getUrlDecoder()
    private val expectedTokenType = OAuth2AccessToken.TokenType.BEARER

    // Custom parser is used, as no signing key available to verify
    fun parseAccessToken(bearerToken: String): OAuth2AccessToken {
      val tokenParts = bearerToken.split(' ')
      val tokenType = OAuth2AccessToken.TokenType(tokenParts[0])
      val tokenValue = tokenParts[1]

      require(tokenType.value == expectedTokenType.value, { "Token type ${tokenType.value} is not supported" })

      return decodeToken(tokenType, tokenValue)
    }

    private fun decodeToken(
      tokenType: OAuth2AccessToken.TokenType,
      tokenValue: String,
    ): OAuth2AccessToken = tokenValue.split(".").let { parts ->
      require(parts.size == 3, { "Invalid token value: $tokenValue" })

      String(decoder.decode(parts[1]))
        .let { payload -> objectMapper.readValue(payload, MNAuthToken::class.java) }
        .let { jwt ->
          val issuedAt = Instant.ofEpochMilli(jwt.created)
          val expiresAt = Instant.ofEpochSecond(jwt.exp)
          OAuth2AccessToken(tokenType, tokenValue, issuedAt, expiresAt)
        }.also { log.trace("Access token will be expiring at = {}", it.expiresAt) }
    }
  }
}

private fun httpClientWireTapLogging() = HttpClient.create().wiretap(
  "reactor.netty.http.client.HttpClient",
  LogLevel.DEBUG,
  AdvancedByteBufFormat.TEXTUAL,
)

private const val DEFAULT_TIMEOUT_SECONDS = 30L
private const val OAUTH2_ERROR_CODE = "invalid_token_response"

private val APPLICATION_JSON_UTF8 = MediaType(
  MediaType.APPLICATION_JSON,
  StandardCharsets.UTF_8,
)
private val ACCEPT_ALL = listOf(MediaType.ALL)
