package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.ExpressionOfInterest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient

private const val EMPLOYERS_ENDPOINT = "/employers"
private const val EMPLOYER_ENDPOINT = "$EMPLOYERS_ENDPOINT/{id}"
private const val JOBS_ENDPOINT = "/jobs"
private const val JOB_ENDPOINT = "$JOBS_ENDPOINT/{id}"
private const val EXPRESSION_OF_INTEREST_ENDPOINT = "/jobs/{jobId}/expressions-of-interest/{prisonNumber}"

@ConditionalOnIntegrationEnabled
@Service
class JobsBoardApiWebClient(
  @Qualifier("jobsBoardWebClient") private val jobsBoardWebClient: WebClient,
) : JobsBoardApiClient {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val employerResponseTypeRef = typeReference<GetEmployersResponse>()
    val jobResponseTypeRef = typeReference<GetJobsResponse>()
  }

  override fun getEmployer(id: String): Employer? {
    log.debug("Getting employer details with id={}", id)
    return jobsBoardWebClient
      .get().uri(EMPLOYER_ENDPOINT, id).accept(APPLICATION_JSON).retrieve()
      .bodyToMono(GetEmployerResponse::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Employer not found. employerId={}; errorResponse={}", id, errorResponse)
        Mono.empty()
      }.block()?.employer()
      .also { log.trace("Employer details: id={}, details={}", id, it) }
  }

  override fun getAllEmployers(page: Int, pageSize: Int): GetEmployersResponse {
    log.debug("Getting all employers; page={}, pageSize={}", page, pageSize)
    return jobsBoardWebClient
      .get().uri {
        it.path(EMPLOYERS_ENDPOINT).queryParam("page", page).queryParam("size", pageSize).build()
      }.accept(APPLICATION_JSON).retrieve()
      .bodyToMono(employerResponseTypeRef)
      .onErrorResume(WebClientResponseException::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Fail to retrieve employers. errorResponse={}", errorResponse)
        Mono.empty()
      }.block()!!
  }

  override fun getJob(id: String): Job? {
    log.debug("Getting job details with id={}", id)
    return jobsBoardWebClient
      .get().uri(JOB_ENDPOINT, id).accept(APPLICATION_JSON).retrieve()
      .bodyToMono(GetJobResponse::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Job not found. jobId={}; errorResponse={}", id, errorResponse)
        Mono.empty()
      }.block()?.job()
  }

  override fun getAllJobs(page: Int, pageSize: Int): GetJobsResponse {
    log.debug("Getting all jobs; page={}, pageSize={}", page, pageSize)
    return jobsBoardWebClient
      .get().uri {
        it.path(JOBS_ENDPOINT).queryParam("page", page).queryParam("size", pageSize).build()
      }.accept(APPLICATION_JSON).retrieve()
      .bodyToMono(jobResponseTypeRef)
      .onErrorResume(WebClientResponseException::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Fail to retrieve jobs. errorResponse={}", errorResponse)
        Mono.empty()
      }.block()!!
  }

  override fun createExpressionOfInterest(expressionOfInterest: ExpressionOfInterest): Unit = expressionOfInterest.let {
    log.debug("Putting job expression-of-interest with jobId={}, prisonNumber={}", it.jobId, it.prisonNumber)
    jobsBoardWebClient.put().uri(EXPRESSION_OF_INTEREST_ENDPOINT, it.jobId, it.prisonNumber).retrieve()
      .toBodilessEntity()
      .onErrorMap(WebClientResponseException.NotFound::class.java) { error ->
        IllegalArgumentException("NotFound: Invalid Prison Number", error)
      }.onErrorMap(WebClientResponseException.BadRequest::class.java) { error ->
        IllegalArgumentException("BadRequest: ${error.responseBodyAsString}", error)
      }.onErrorMap(WebClientResponseException::class.java) { error ->
        "Fail to create expression-of-interest! errorMessage=${error.message} ; errorResponse=$error.responseBodyAsString".let {
          Exception(it, error)
        }
      }.block()
  }
}

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
