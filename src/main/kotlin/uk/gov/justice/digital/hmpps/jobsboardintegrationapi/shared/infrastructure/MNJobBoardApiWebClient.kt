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
private const val JOBS_ENDPOINT = "/jobs-prison-leavers"
private const val UPDATE_JOBS_ENDPOINT = JOBS_ENDPOINT

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
      .onErrorMap { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Exception("Fail to create employer! errorResponse=$errorResponse", error)
      }.block()!!
      .responseObject
      .also { log.trace("Create employer response={}", it) }
  }

  override fun updateEmployer(request: UpdateEmployerRequest): UpdateEmployerResponse {
    log.debug("Updating employer employerName={}, id={}", request.employerName, request.id)
    log.trace("Update employer request={}", request)
    return mnJobBoardWebClient.post().uri(UPDATE_EMPLOYERS_ENDPOINT, request.id)
      .accept(APPLICATION_JSON).body(Mono.just(request), request.javaClass)
      .retrieve()
      .bodyToMono(MNUpdateEmployerResponse::class.java)
      .onErrorMap { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Exception("Fail to update employer! errorResponse=$errorResponse", error)
      }.block()!!
      .responseObject
      .also { log.trace("Update employer response={}", it) }
  }

  override fun createJob(request: CreateJobRequest): CreateJobResponse {
    log.debug("Creating job jobTitle={}, employerId={}", request.jobTitle, request.employerId)
    log.trace("Create job request={}", request)
    return mnJobBoardWebClient.post().uri(JOBS_ENDPOINT)
      .accept(APPLICATION_JSON).body(Mono.just(request.mnRequest()), MNCreateJobRequest::class.java)
      .retrieve()
      .bodyToMono(MNCreateJobResponse::class.java)
      .onErrorMap { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Exception("Fail to create job! errorResponse=$errorResponse", error)
      }.block()!!
      .responseObject
  }

  override fun updateJob(request: UpdateJobRequest): UpdateJobResponse {
    log.debug("Updating job jobTitle={}, employerId={}", request.jobTitle, request.employerId)
    log.trace("Update job request={}", request)
    return mnJobBoardWebClient.put().uri(UPDATE_JOBS_ENDPOINT)
      .accept(APPLICATION_JSON).body(Mono.just(request.mnRequest()), MNUpdateJobRequest::class.java)
      .retrieve()
      .bodyToMono(MNUpdateJobResponse::class.java)
      .onErrorMap { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        Exception("Fail to update job! errorResponse=$errorResponse", error)
      }.block()!!
      .responseObject
  }
}
