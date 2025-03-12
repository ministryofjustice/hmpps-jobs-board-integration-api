package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient.Companion.FETCH_SIZE
import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil

data class GetEmployerResponse(
  val id: String,
  val name: String,
  val description: String,
  val sector: String,
  val status: String,
  val createdAt: String,
) {
  companion object {
    fun from(employer: Employer): GetEmployerResponse = GetEmployerResponse(
      id = employer.id,
      name = employer.name,
      description = employer.description,
      sector = employer.sector,
      status = employer.status,
      createdAt = employer.createdAt.toString(),
    )
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

typealias GetEmployersResponse = PageResponse<GetEmployerResponse>

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
    fun from(job: Job): GetJobResponse = GetJobResponse(
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

    private fun String.asList(): List<String> = this.split(",").map { it.trim() }.toList()
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

data class PageResponse<T>(
  val content: List<T>,
  val page: PageMetaData,
) {
  constructor() : this(emptyList(), PageMetaData(0, FETCH_SIZE, 0, 0))

  companion object {
    fun <T> from(
      vararg items: T,
      number: Int = 0,
      size: Int = FETCH_SIZE,
      totalElements: Long = items.size.toLong(),
    ): PageResponse<T> {
      val totalPages = ceil(totalElements.toDouble() / size).toInt()
      return PageResponse(
        content = items.toList(),
        page = PageMetaData(size, number, totalElements, totalPages),
      )
    }
  }

  fun isEmpty() = page.totalElements <= 0
  fun hasNext() = page.number < page.totalPages - 1
}

data class PageMetaData(
  val size: Int,
  val number: Int,
  val totalElements: Long,
  val totalPages: Int,
)
