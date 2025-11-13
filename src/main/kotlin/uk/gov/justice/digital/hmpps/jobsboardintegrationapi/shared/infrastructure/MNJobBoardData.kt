package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

data class CreateEmployerRequest(
  val employerName: String,
  val employerBio: String,
  val sectorId: Int,
  val partnerId: Int,
  val imgName: String? = null,
  val path: String? = null,
) {
  companion object {
    fun from(employer: MNEmployer) = employer.run { CreateEmployerRequest(employerName, employerBio, sectorId, partnerId, imgName, path) }
  }
}

typealias CreateEmployerResponse = MNEmployer

data class UpdateEmployerRequest(
  val id: Long,
  val employerName: String,
  val employerBio: String,
  val sectorId: Int,
  val partnerId: Int,
  val imgName: String? = null,
  val path: String? = null,
) {
  companion object {
    fun from(employer: MNEmployer) = employer.run { UpdateEmployerRequest(id!!, employerName, employerBio, sectorId, partnerId, imgName, path) }
  }
}

typealias UpdateEmployerResponse = MNEmployer

data class CreateJobRequest(
  val jobTitle: String,
  val jobDescription: String,
  val closingDate: String? = null,
  val jobTypeId: Int,
  val charityId: Int? = null,
  val excludingOffenceIds: List<Int> = emptyList(),
  val excludingOffenceOther: String? = null,
  val employerId: Long,
  val jobSourceOneId: Int,
  val jobSourceTwoId: Int? = null,
  val employerSectorId: Int,
  val workPatternId: Int,
  val contractTypeId: Int,
  val hoursId: Int,
  val rollingOpportunity: Boolean,
  val baseLocationId: Int? = null,
  val postcode: String? = null,
  val salaryFrom: String,
  val salaryTo: String? = null,
  val salaryPeriodId: Int,
  val additionalSalaryInformation: String? = null,
  val nationalMinimumWage: Boolean,
  val ringfencedJob: Boolean? = null,
  val desirableJobCriteria: String? = null,
  val essentialJobCriteria: String,
  val howToApply: String,
) {
  companion object {
    fun from(job: MNJob) = job.run {
      CreateJobRequest(
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        closingDate = closingDate,
        jobTypeId = 1,
        charityId = null,
        excludingOffenceIds = excludingOffences.choiceIds,
        excludingOffenceOther = excludingOffences.other,
        employerId = employerId,
        jobSourceOneId = jobSourceOneId,
        jobSourceTwoId = jobSourceTwoList?.firstOrNull(),
        employerSectorId = employerSectorId,
        workPatternId = workPatternId,
        contractTypeId = contractTypeId,
        hoursId = hoursId,
        rollingOpportunity = rollingOpportunity,
        baseLocationId = baseLocationId,
        postcode = postcode,
        salaryFrom = salaryFrom,
        salaryTo = salaryTo,
        salaryPeriodId = salaryPeriodId,
        additionalSalaryInformation = additionalSalaryInformation,
        nationalMinimumWage = nationalMinimumWage,
        ringfencedJob = ringfencedJob,
        desirableJobCriteria = desirableJobCriteria,
        essentialJobCriteria = essentialJobCriteria,
        howToApply = howToApply,
      )
    }
  }

  fun mnRequest() = MNCreateJobRequest(
    id = null,
    jobTitle = jobTitle,
    jobDescription = jobDescription,
    postingDate = null,
    jobTypeId = 1,
    charityId = null,
    excludingOffences = MNExcludingOffences(
      choiceIds = excludingOffenceIds,
      other = excludingOffenceOther,
    ),
    employerId = employerId,
    jobSourceOneId = jobSourceOneId,
    jobSourceTwoList = jobSourceTwoId?.let { listOf(it) },
    employerSectorId = employerSectorId,
    workPatternId = workPatternId,
    contractTypeId = contractTypeId,
    hoursId = hoursId,
    rollingOpportunity = rollingOpportunity,
    baseLocationId = baseLocationId,
    postcode = postcode,
    salaryFrom = salaryFrom,
    salaryTo = salaryTo,
    salaryPeriodId = salaryPeriodId,
    additionalSalaryInformation = additionalSalaryInformation,
    nationalMinimumWage = nationalMinimumWage,
    ringfencedJob = ringfencedJob,
    desirableJobCriteria = desirableJobCriteria,
    essentialJobCriteria = essentialJobCriteria,
    howToApply = howToApply,
  )
}

typealias CreateJobResponse = MNJob

data class UpdateJobRequest(
  val id: Long,
  val jobTitle: String,
  val jobDescription: String,
  val closingDate: String? = null,
  val jobTypeId: Int,
  val charityId: Int? = null,
  val excludingOffenceIds: List<Int> = emptyList(),
  val excludingOffenceOther: String? = null,
  val employerId: Long,
  val jobSourceOneId: Int,
  val jobSourceTwoId: Int? = null,
  val employerSectorId: Int,
  val workPatternId: Int,
  val contractTypeId: Int,
  val hoursId: Int,
  val rollingOpportunity: Boolean,
  val baseLocationId: Int? = null,
  val postcode: String? = null,
  val salaryFrom: String,
  val salaryTo: String? = null,
  val salaryPeriodId: Int,
  val additionalSalaryInformation: String? = null,
  val nationalMinimumWage: Boolean,
  val ringfencedJob: Boolean? = null,
  val desirableJobCriteria: String? = null,
  val essentialJobCriteria: String,
  val howToApply: String,
) {
  companion object {
    fun from(job: MNJob) = job.run {
      UpdateJobRequest(
        id = id!!,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        closingDate = closingDate,
        jobTypeId = 1,
        charityId = null,
        excludingOffenceIds = excludingOffences.choiceIds,
        excludingOffenceOther = excludingOffences.other,
        employerId = employerId,
        jobSourceOneId = jobSourceOneId,
        jobSourceTwoId = jobSourceTwoList?.firstOrNull(),
        employerSectorId = employerSectorId,
        workPatternId = workPatternId,
        contractTypeId = contractTypeId,
        hoursId = hoursId,
        rollingOpportunity = rollingOpportunity,
        baseLocationId = baseLocationId,
        postcode = postcode,
        salaryFrom = salaryFrom,
        salaryTo = salaryTo,
        salaryPeriodId = salaryPeriodId,
        additionalSalaryInformation = additionalSalaryInformation,
        nationalMinimumWage = nationalMinimumWage,
        ringfencedJob = ringfencedJob,
        desirableJobCriteria = desirableJobCriteria,
        essentialJobCriteria = essentialJobCriteria,
        howToApply = howToApply,
      )
    }
  }

  fun mnRequest() = MNUpdateJobRequest(
    id = id,
    jobTitle = jobTitle,
    jobDescription = jobDescription,
    postingDate = null,
    closingDate = closingDate,
    jobTypeId = 1,
    charityId = null,
    excludingOffences = MNExcludingOffences(
      choiceIds = excludingOffenceIds,
      other = excludingOffenceOther,
    ),
    employerId = employerId,
    jobSourceOneId = jobSourceOneId,
    jobSourceTwoList = jobSourceTwoId?.let { listOf(it) },
    employerSectorId = employerSectorId,
    workPatternId = workPatternId,
    contractTypeId = contractTypeId,
    hoursId = hoursId,
    rollingOpportunity = rollingOpportunity,
    baseLocationId = baseLocationId,
    postcode = postcode,
    salaryFrom = salaryFrom,
    salaryTo = salaryTo,
    salaryPeriodId = salaryPeriodId,
    additionalSalaryInformation = additionalSalaryInformation,
    nationalMinimumWage = nationalMinimumWage,
    ringfencedJob = ringfencedJob,
    desirableJobCriteria = desirableJobCriteria,
    essentialJobCriteria = essentialJobCriteria,
    howToApply = howToApply,
  )
}

typealias UpdateJobResponse = MNJob

typealias MNCreateEmployerResponse = MNCreateOrUpdateEmployerResponse
typealias MNUpdateEmployerResponse = MNCreateOrUpdateEmployerResponse

class MNCreateOrUpdateEmployerResponse(message: MNMessage, responseObject: MNEmployer) : MNCreateOrUpdateDataResponse<MNEmployer>(message, responseObject)

typealias MNCreateJobRequest = MNJob
typealias MNCreateJobResponse = MNCreateOrUpdateJobResponse

typealias MNUpdateJobRequest = MNJob
typealias MNUpdateJobResponse = MNCreateOrUpdateJobResponse

class MNCreateOrUpdateJobResponse(message: MNMessage, responseObject: MNJob) : MNCreateOrUpdateDataResponse<MNJob>(message, responseObject)

abstract class MNCreateOrUpdateDataResponse<T>(val message: MNMessage, val responseObject: T)

data class MNEmployer(
  val id: Long? = null,
  val employerName: String,
  val employerBio: String,
  val sectorId: Int,
  val partnerId: Int,
  val imgName: String? = null,
  val path: String? = null,
)

data class MNJob(
  val id: Long? = null,
  val jobTitle: String,
  val jobDescription: String,
  val postingDate: String? = null,
  val closingDate: String? = null,
  val jobTypeId: Int,
  val charityId: Int? = null,
  val excludingOffences: MNExcludingOffences,
  val employerId: Long,
  val jobSourceOneId: Int,
  val jobSourceTwoList: List<Int>? = null,
  val employerSectorId: Int,
  val workPatternId: Int,
  val contractTypeId: Int,
  val hoursId: Int,
  val rollingOpportunity: Boolean,
  val baseLocationId: Int? = null,
  val postcode: String? = null,
  val salaryFrom: String,
  val salaryTo: String? = null,
  val salaryPeriodId: Int,
  val additionalSalaryInformation: String? = null,
  val nationalMinimumWage: Boolean,
  val ringfencedJob: Boolean? = null,
  val desirableJobCriteria: String? = null,
  val essentialJobCriteria: String,
  val howToApply: String,
  val isNational: Boolean = false,
)

data class MNExcludingOffences(
  val choiceIds: List<Int> = emptyList(),
  val other: String? = null,
)

data class MNMessage(
  val successCode: String,
  val successMessage: String,
  val httpStatusCode: Int,
)
