package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest

interface MNAuthApiClient : OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>

interface MNAuthClientManager : OAuth2AuthorizedClientManager

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MNAuthGrantRequest(
  val appId: Long,
  val userName: String,
  val password: String,
)

data class MNAuthToken(
  val sub: String,
  val created: Long,
  val exp: Long,
  val role: String? = null,
  val userName: String? = null,
  val isSystemUser: Boolean? = null,
  val userId: Long? = null,
)

data class MNApiError(
  val errorCode: String? = null,
  val errorMessage: String? = null,
  val httpStatusCode: Int? = null,
)
