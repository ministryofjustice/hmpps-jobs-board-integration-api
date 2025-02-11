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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import java.time.Instant
import java.time.LocalDate

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

  override fun getJob(id: String): Job? {
    log.debug("Getting job details with id={}", id)
    return jobsBoardWebClient
      .get().uri("/jobs/{id}", id).accept(APPLICATION_JSON).retrieve()
      .bodyToMono(GetJobResponse::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { error ->
        val errorResponse = if (error is WebClientResponseException) error.responseBodyAsString else null
        log.warn("Job not found. jobId={}; errorResponse={}", id, errorResponse)
        Mono.empty()
      }.block()?.job()
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

data class GetJobResponse(
  val id: String,
  val employerId: String,
  val jobTitle: String,
  val sector: String,
  val industrySector: String,
  val numberOfVacancies: Int,
  val sourcePrimary: String,
  val sourceSecondary: String? = null,
  val charityName: String? = null,
  val postCode: String,
  val salaryFrom: Double,
  val salaryTo: Double? = null,
  val salaryPeriod: String,
  val additionalSalaryInformation: String? = null,
  val isPayingAtLeastNationalMinimumWage: Boolean,
  val workPattern: String,
  val hoursPerWeek: String,
  val contractType: String,
  val baseLocation: String? = null,
  val essentialCriteria: String,
  val desirableCriteria: String? = null,
  val description: String,
  val offenceExclusions: List<String>,
  val offenceExclusionsDetails: String? = null,
  val isRollingOpportunity: Boolean,
  val closingDate: String? = null,
  val isOnlyForPrisonLeavers: Boolean,
  val startDate: String? = null,
  val howToApply: String,
  val supportingDocumentationRequired: List<String>? = null,
  val supportingDocumentationDetails: String? = null,
  val createdAt: String,
) {
  companion object {
    fun from(job: Job): GetJobResponse {
      return GetJobResponse(
        id = job.id,
        employerId = job.employerId,
        jobTitle = job.title,
        sector = job.sector,
        industrySector = job.industrySector,
        numberOfVacancies = job.numberOfVacancies,
        sourcePrimary = job.sourcePrimary,
        sourceSecondary = job.sourceSecondary,
        charityName = job.charityName,
        postCode = job.postcode,
        salaryFrom = job.salaryFrom,
        salaryTo = job.salaryTo,
        salaryPeriod = job.salaryPeriod,
        additionalSalaryInformation = job.additionalSalaryInformation,
        isPayingAtLeastNationalMinimumWage = job.isPayingAtLeastNationalMinimumWage,
        workPattern = job.workPattern,
        hoursPerWeek = job.hoursPerWeek,
        contractType = job.contractType,
        baseLocation = job.baseLocation,
        essentialCriteria = job.essentialCriteria,
        desirableCriteria = job.desirableCriteria,
        description = job.description,
        offenceExclusions = job.offenceExclusions.asList(),
        offenceExclusionsDetails = job.offenceExclusionsDetails,
        isRollingOpportunity = job.isRollingOpportunity,
        closingDate = job.closingDate?.toString(),
        isOnlyForPrisonLeavers = job.isOnlyForPrisonLeavers,
        startDate = job.startDate?.toString(),
        howToApply = job.howToApply,
        supportingDocumentationRequired = job.supportingDocumentationRequired?.asList(),
        supportingDocumentationDetails = job.supportingDocumentationDetails,
        createdAt = job.createdAt.toString(),
      )
    }

    private fun String.asList(): List<String> {
      return this.split(",").map { it.trim() }.toList()
    }
  }

  fun job() = Job(
    id = id,
    title = jobTitle,
    sector = sector,
    industrySector = industrySector,
    numberOfVacancies = numberOfVacancies,
    sourcePrimary = sourcePrimary,
    sourceSecondary = sourceSecondary,
    charityName = charityName,
    postcode = postCode,
    salaryFrom = salaryFrom,
    salaryTo = salaryTo,
    salaryPeriod = salaryPeriod,
    additionalSalaryInformation = additionalSalaryInformation,
    isPayingAtLeastNationalMinimumWage = isPayingAtLeastNationalMinimumWage,
    workPattern = workPattern,
    hoursPerWeek = hoursPerWeek,
    contractType = contractType,
    baseLocation = baseLocation,
    essentialCriteria = essentialCriteria,
    desirableCriteria = desirableCriteria,
    description = description,
    offenceExclusions = joinToString(offenceExclusions)!!,
    offenceExclusionsDetails = offenceExclusionsDetails,
    isRollingOpportunity = isRollingOpportunity,
    closingDate = localDate(closingDate),
    isOnlyForPrisonLeavers = isOnlyForPrisonLeavers,
    startDate = localDate(startDate),
    howToApply = howToApply,
    supportingDocumentationRequired = joinToString(supportingDocumentationRequired),
    supportingDocumentationDetails = supportingDocumentationDetails,
    employerId = employerId,
    createdAt = instant(createdAt),
  )

  private fun localDate(date: String?) = date?.let { LocalDate.parse(it) }
  private fun instant(time: String?) = time?.let { Instant.parse(it) }
  private fun joinToString(listOfStrings: List<String>?) = listOfStrings?.joinToString(",")
}
