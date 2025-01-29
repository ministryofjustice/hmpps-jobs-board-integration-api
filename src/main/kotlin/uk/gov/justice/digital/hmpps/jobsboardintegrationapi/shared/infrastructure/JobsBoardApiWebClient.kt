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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import java.time.Instant

@ConditionalOnIntegrationEnabled
@Service
class JobsBoardApiWebClient(
  @Qualifier("jobsBoardWebClient") private val jobsBoardWebClient: WebClient,
) : JobsBoardApiClient {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getEmployer(id: String): Employer? {
    log.debug("Getting employer details with id={}", id)
    return jobsBoardWebClient
      .get().uri("/employers/{id}", id).accept(APPLICATION_JSON).retrieve()
      .bodyToMono(GetEmployerResponse::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Employer not found. employerId={}; errorResponse={}", id, errorResponse)
        Mono.empty()
      }.block()?.employer()
  }
}

data class GetEmployerResponse(
  val id: String,
  val name: String,
  val description: String,
  val sector: String,
  val status: String,
  val createdAt: String,
) {
  companion object {
    fun from(employer: Employer): GetEmployerResponse {
      return GetEmployerResponse(
        id = employer.id,
        name = employer.name,
        description = employer.description,
        sector = employer.sector,
        status = employer.status,
        createdAt = employer.createdAt.toString(),
      )
    }
  }

  fun employer() = Employer(
    id = id,
    name = name,
    description = description,
    sector = sector,
    status = status,
    createdAt = Instant.parse(createdAt),
  )
}
