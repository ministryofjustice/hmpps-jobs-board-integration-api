package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNAuthToken
import java.security.KeyPair
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

private const val URL_PREFIX = "/mn-auth"
private const val TOKEN_URI = "$URL_PREFIX/systemlogin"

class MnAuthMockServer : WireMockServer(WIREMOCK_PORT) {
  private val mapper: JsonMapper

  private val encoder: Base64.Encoder
  private val jwtSigningKey: KeyPair

  companion object {
    private const val WIREMOCK_PORT = 8091
  }

  init {
    mapper = jsonMapper()
    encoder = Base64.getUrlEncoder()
    jwtSigningKey = Jwts.SIG.RS512.keyPair().build()
  }

  fun stubGrantToken() {
    stubFor(
      post(urlEqualTo(TOKEN_URI))
        .willReturn(
          aResponse()
            .withHeaders(
              HttpHeaders(
                listOf(
                  HttpHeader("Access-Control-Expose-Headers", "X-Authorization"),
                  HttpHeader("X-Authorization", bearerToken()),
                ),
              ),
            ),
        ),
    )
  }

  private fun bearerToken(): String {
    val username = "mn_user"
    val tzOffset = ZoneOffset.UTC
    val createTime = LocalDateTime.now().toInstant(tzOffset)

    return MNAuthToken(
      sub = username,
      created = createTime.toEpochMilli(),
      exp = createTime.plusSeconds(3_600 * 2).epochSecond,
      role = "mn_role",
      userName = username,
      isSystemUser = true,
      userId = SecureRandom().nextLong(),
    ).let { "Bearer ${it.jwt()}" }
  }

  private fun MNAuthToken.jwt(): String = mapper.writeValueAsString(this).let { token ->
    Jwts.builder()
      .content(token)
      .signWith(jwtSigningKey.private)
      .compact()
  }
}

class MnAuthApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val mnAuth = MnAuthMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    mnAuth.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    mnAuth.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    mnAuth.stop()
  }
}
