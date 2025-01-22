package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient

private const val EMPLOYERS_ENDPOINT = "/employers"

@ConditionalOnIntegrationEnabled
@Service
class MNJobBoardApiWebClient(
  @Qualifier("mnJobBoardWebClient") private val mnJobBoardWebClient: WebClient,
) : MNJobBoardApiClient {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun createEmployer(request: CreatEmployerRequest): CreatEmployerResponse {
    log.debug("Creating employer employerName={}", request.employerName)
    return mnJobBoardWebClient.post().uri(EMPLOYERS_ENDPOINT).accept(APPLICATION_JSON).retrieve()
      .bodyToMono(MNCreatEmployerResponse::class.java)
      .onErrorResume { error -> Mono.error(Exception("Fail to create employer", error)) }.block()!!
      .responseObject
  }
}
