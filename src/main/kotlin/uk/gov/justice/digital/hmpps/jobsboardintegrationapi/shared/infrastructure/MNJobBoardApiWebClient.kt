package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient

private const val EMPLOYERS_ENDPOINT = "/employers"
private const val UPDATE_EMPLOYERS_ENDPOINT = "$EMPLOYERS_ENDPOINT/{id}"

@ConditionalOnIntegrationEnabled
@Service
class MNJobBoardApiWebClient(
  @Qualifier("mnJobBoardWebClient") private val mnJobBoardWebClient: WebClient,
) : MNJobBoardApiClient {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun createEmployer(request: CreateEmployerRequest): CreateEmployerResponse {
    log.debug("Creating employer employerName={}", request.employerName)
    log.trace("Create employer request={}", request)
    return mnJobBoardWebClient.post().uri(EMPLOYERS_ENDPOINT)
      .accept(APPLICATION_JSON).body(Mono.just(request), request.javaClass)
      .retrieve()
      .bodyToMono(MNCreateEmployerResponse::class.java)
      .onErrorResume { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Mono.error(Exception("Fail to create employer! errorResponse=$errorResponse", error))
      }.block()!!
      .responseObject
  }

  override fun updateEmployer(request: UpdateEmployerRequest): UpdateEmployerResponse {
    log.debug("Updating employer employerName={}, id={}", request.employerName, request.id)
    log.trace("Update employer request={}", request)
    return mnJobBoardWebClient.post().uri(UPDATE_EMPLOYERS_ENDPOINT, request.id)
      .accept(APPLICATION_JSON).body(Mono.just(request), request.javaClass)
      .retrieve()
      .bodyToMono(MNUpdateEmployerResponse::class.java)
      .onErrorResume { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Mono.error(Exception("Fail to update employer! errorResponse=$errorResponse", error))
      }.block()!!
      .responseObject
  }
}
