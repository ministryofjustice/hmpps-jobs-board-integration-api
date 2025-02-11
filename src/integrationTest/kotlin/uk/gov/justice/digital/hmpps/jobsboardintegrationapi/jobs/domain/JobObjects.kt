package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.abcConstruction
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.employerSectorIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.baseLocationIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.contractTypeIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.hoursPerWeekIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.jobSourceIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.offenceExclusionIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.salaryPeriodIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.workPatternIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNExcludingOffences
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob
import java.time.LocalDate
import java.util.*

internal object JobObjects {
  val tescoWarehouseHandler = Job(
    id = "04295747-e60d-4e51-9716-e721a63bdd06",
    title = "Warehouse handler",
    sector = "WAREHOUSING",
    industrySector = "LOGISTICS",
    numberOfVacancies = 1,
    sourcePrimary = "DWP",
    sourceSecondary = null,
    charityName = null,
    postcode = "NE15 7LR",
    salaryFrom = 1_234_567.12,
    salaryTo = 7_654_321.12,
    salaryPeriod = "PER_DAY",
    additionalSalaryInformation = null,
    isPayingAtLeastNationalMinimumWage = true,
    workPattern = "FLEXIBLE_SHIFTS",
    contractType = "TEMPORARY",
    hoursPerWeek = "FULL_TIME_40_PLUS",
    baseLocation = "HYBRID",
    essentialCriteria = "Essential job criteria",
    desirableCriteria = null,
    description = "Job description\r\nDescribe the role and main tasks. Include any benefits and training opportunities.",
    offenceExclusions = "CASE_BY_CASE,OTHER",
    offenceExclusionsDetails = null,
    howToApply = "How to applyHow to apply",
    closingDate = null,
    startDate = null,
    isRollingOpportunity = false,
    isOnlyForPrisonLeavers = true,
    supportingDocumentationRequired = "DISCLOSURE_LETTER,OTHER",
    supportingDocumentationDetails = null,
    employer = tesco,
  )

  val amazonForkliftOperator = Job(
    id = "d3035924-f9fe-426f-b253-f7c8225167ae",
    title = "Forklift operator",
    sector = "RETAIL",
    industrySector = "LOGISTICS",
    numberOfVacancies = 2,
    sourcePrimary = "PEL",
    sourceSecondary = null,
    charityName = "",
    postcode = "LS12 1AA",
    salaryFrom = 11.93,
    salaryTo = 15.90,
    salaryPeriod = "PER_HOUR",
    additionalSalaryInformation = null,
    isPayingAtLeastNationalMinimumWage = false,
    workPattern = "FLEXIBLE_SHIFTS",
    hoursPerWeek = "FULL_TIME",
    contractType = "TEMPORARY",
    baseLocation = "WORKPLACE",
    essentialCriteria = "",
    desirableCriteria = "",
    description = """
      What's on offer:

      - 5 days over 7, 05:30 to 15:30
      - Paid weekly
      - Immediate starts available
      - Full training provided
      
      Your duties will include:

      - Manoeuvring forklifts safely in busy industrial environments
      - Safely stacking and unstacking large quantities of goods onto shelves or pallets
      - Moving goods from storage areas to loading areas for transport
      - Unloading deliveries and safely relocating the goods to their designated storage areas
      - Ensuring forklift driving areas are free from spills or obstructions
      - Regularly checking forklift equipment for faults or damages
      - Consolidating partial pallets for incoming goods
    """.trimIndent(),
    offenceExclusions = "NONE,DRIVING,OTHER",
    offenceExclusionsDetails = "Other offence A, another offence B, yet another offence C",
    isRollingOpportunity = false,
    closingDate = LocalDate.parse("2025-02-01"),
    isOnlyForPrisonLeavers = true,
    startDate = LocalDate.parse("2025-05-31"),
    howToApply = "",
    supportingDocumentationRequired = "CV,DISCLOSURE_LETTER",
    supportingDocumentationDetails = "",
    employer = amazon,
  )

  val abcConstructionApprentice = Job(
    id = "6fdf2bf4-cfe6-419c-bab2-b3673adbb393",
    title = "Apprentice plasterer",
    sector = "CONSTRUCTION",
    industrySector = "CONSTRUCTION",
    numberOfVacancies = 3,
    sourcePrimary = "DWP",
    sourceSecondary = null,
    charityName = null,
    postcode = "NE15 7LR",
    salaryFrom = 99.0,
    salaryTo = null,
    salaryPeriod = "PER_DAY",
    additionalSalaryInformation = null,
    isPayingAtLeastNationalMinimumWage = true,
    workPattern = "FLEXIBLE_SHIFTS",
    contractType = "TEMPORARY",
    hoursPerWeek = "FULL_TIME_40_PLUS",
    baseLocation = null,
    essentialCriteria = "Essential job criteria",
    desirableCriteria = null,
    description = "Job description\r\nDescribe the role and main tasks. Include any benefits and training opportunities.",
    offenceExclusions = "CASE_BY_CASE,OTHER",
    offenceExclusionsDetails = null,
    howToApply = "How to applyHow to apply",
    closingDate = null,
    startDate = null,
    isRollingOpportunity = false,
    isOnlyForPrisonLeavers = true,
    supportingDocumentationRequired = "DISCLOSURE_LETTER,OTHER",
    supportingDocumentationDetails = null,
    employer = abcConstruction,
  )

  val jobSourceIdMap = mapOf(
    "DWP" to 4,
    "EAB" to 14,
    "EDUCATION" to 15,
    "IAG" to 8,
    "NFN" to 1,
    "PRISON" to 16,
    "THIRD_SECTOR" to 10,
    "PEL" to 2,
    "OTHER" to 11,
  )

  val salaryPeriodIdMap = mapOf(
    "PER_DAY" to 2,
    "PER_FORTNIGHT" to 4,
    "PER_HOUR" to 1,
    "PER_MONTH" to 5,
    "PER_WEEK" to 3,
    "PER_YEAR" to 6,
    "PER_YEAR_PRO_RATA" to 7,
  )

  val workPatternIdMap = mapOf(
    "ANNUALISED_HOURS" to 1,
    "COMPRESSED_HOURS" to 2,
    "FLEXI_TIME" to 3,
    "FLEXIBLE_SHIFTS" to 4,
    "JOB_SHARE" to 5,
    "STAGGERED_HOURS" to 6,
    "TERM_TIME_HOURS" to 7,
    "UNSOCIABLE_HOURS" to 8,
  )

  val contractTypeIdMap = mapOf(
    "FIXED_TERM_CONTRACT" to 4,
    "PERMANENT" to 1,
    "SELF_EMPLOYMENT" to 3,
    "TEMPORARY" to 2,
  )

  val hoursPerWeekIdMap = mapOf(
    "FULL_TIME" to 2,
    "FULL_TIME_40_PLUS" to 1,
    "PART_TIME" to 3,
    "ZERO_HOURS" to 4,
  )

  val baseLocationIdMap = mapOf(
    "REMOTE" to 1,
    "HYBRID" to 3,
    "WORKPLACE" to 2,
  )

  val offenceExclusionIdMap = mapOf(
    "NONE" to 1,
    "CASE_BY_CASE" to 15,
    "ARSON" to 16,
    "DRIVING" to 17,
    "MURDER" to 18,
    "SEXUAL" to 3,
    "TERRORISM" to 19,
    "OTHER" to 14,
  )

  fun builder(): JobBuilder {
    return JobBuilder()
  }
}

class JobBuilder {
  var id: String = UUID.randomUUID().toString()
  var title: String = "Service Colleague"
  var sector: String = "RETAIL"
  var industrySector: String = "RETAIL"
  var numberOfVacancies: Int = 1
  var sourcePrimary: String = "PEL"
  var sourceSecondary: String? = null
  var charityName: String? = null
  var postcode: String = "LS11 5AD"
  var salaryFrom: Double = 96.32
  var salaryTo: Double? = null
  var salaryPeriod: String = "PER_DAY"
  var additionalSalaryInformation: String? = null
  var isPayingAtLeastNationalMinimumWage: Boolean = true
  var workPattern: String = "FLEXIBLE_SHIFTS"
  var hoursPerWeek: String = "FULL_TIME"
  var contractType: String = "PERMANENT"
  var baseLocation: String? = null
  var essentialCriteria: String = "Essential job criteria"
  var desirableCriteria: String? = null
  var description: String = ""
  var offenceExclusions: String = "CASE_BY_CASE,OTHER"
  var offenceExclusionsDetails: String? = null
  var isRollingOpportunity: Boolean = false
  var closingDate: LocalDate? = null
  var isOnlyForPrisonLeavers: Boolean = true
  var startDate: LocalDate? = null
  var howToApply: String = "How to apply How to apply"
  var supportingDocumentationRequired: String? = null
  var supportingDocumentationDetails: String? = null
  var expressionsOfInterest: MutableMap<String, ExpressionOfInterest> = mutableMapOf()
  var employerId: String = UUID.randomUUID().toString()
  var employer: Employer? = Employer(
    id = employerId,
    name = "ASDA",
    description = "Asda and often styled as ASDA, is a British supermarket and petrol station chain. Its headquarters are in Leeds, England.",
    sector = "RETAIL",
    status = "SILVER",
  )

  fun withId(id: String): JobBuilder {
    this.id = id
    return this
  }

  fun withExpressionsOfInterest(expressionsOfInterest: MutableMap<String, ExpressionOfInterest>): JobBuilder {
    this.expressionsOfInterest = expressionsOfInterest
    return this
  }

  fun withExpressionOfInterestFrom(prisonNumber: String): JobBuilder {
    this.expressionsOfInterest.put(
      prisonNumber,
      ExpressionOfInterest(
        jobId = this.id,
        prisonNumber,
      ),
    )
    return this
  }

  fun from(job: Job): JobBuilder {
    this.id = job.id
    this.title = job.title
    this.sector = job.sector
    this.industrySector = job.industrySector
    this.numberOfVacancies = job.numberOfVacancies
    this.sourcePrimary = job.sourcePrimary
    this.sourceSecondary = job.sourceSecondary
    this.charityName = job.charityName
    this.postcode = job.postcode
    this.salaryFrom = job.salaryFrom
    this.salaryTo = job.salaryTo
    this.salaryPeriod = job.salaryPeriod
    this.additionalSalaryInformation = job.additionalSalaryInformation
    this.isPayingAtLeastNationalMinimumWage = job.isPayingAtLeastNationalMinimumWage
    this.workPattern = job.workPattern
    this.hoursPerWeek = job.hoursPerWeek
    this.contractType = job.contractType
    this.baseLocation = job.baseLocation
    this.essentialCriteria = job.essentialCriteria
    this.desirableCriteria = job.desirableCriteria
    this.description = job.description
    this.offenceExclusions = job.offenceExclusions
    this.offenceExclusionsDetails = job.offenceExclusionsDetails
    this.isRollingOpportunity = job.isRollingOpportunity
    this.closingDate = job.closingDate
    this.isOnlyForPrisonLeavers = job.isOnlyForPrisonLeavers
    this.startDate = job.startDate
    this.howToApply = job.howToApply
    this.supportingDocumentationRequired = job.supportingDocumentationRequired
    this.supportingDocumentationDetails = job.supportingDocumentationDetails
    this.expressionsOfInterest = job.expressionsOfInterest.mapValues { entry ->
      ExpressionOfInterest(
        jobId = entry.value.jobId,
        prisonNumber = entry.value.prisonNumber,
      )
    }.toMutableMap()
    this.employerId = job.employerId
    this.employer = job.employer?.run { Employer(id, name, description, sector, status) }
    return this
  }

  fun build(): Job {
    return Job(
      id = this.id,
      title = this.title,
      sector = this.sector,
      industrySector = this.industrySector,
      numberOfVacancies = this.numberOfVacancies,
      sourcePrimary = this.sourcePrimary,
      sourceSecondary = this.sourceSecondary,
      charityName = this.charityName,
      postcode = this.postcode,
      salaryFrom = this.salaryFrom,
      salaryTo = this.salaryTo,
      salaryPeriod = this.salaryPeriod,
      additionalSalaryInformation = this.additionalSalaryInformation,
      isPayingAtLeastNationalMinimumWage = this.isPayingAtLeastNationalMinimumWage,
      workPattern = this.workPattern,
      hoursPerWeek = this.hoursPerWeek,
      contractType = this.contractType,
      baseLocation = this.baseLocation,
      essentialCriteria = this.essentialCriteria,
      desirableCriteria = this.desirableCriteria,
      description = this.description,
      offenceExclusions = this.offenceExclusions,
      offenceExclusionsDetails = this.offenceExclusionsDetails,
      isRollingOpportunity = this.isRollingOpportunity,
      closingDate = this.closingDate,
      isOnlyForPrisonLeavers = this.isOnlyForPrisonLeavers,
      startDate = this.startDate,
      howToApply = this.howToApply,
      supportingDocumentationRequired = this.supportingDocumentationRequired,
      supportingDocumentationDetails = this.supportingDocumentationDetails,
      employerId = this.employerId,
      expressionsOfInterest = this.expressionsOfInterest,
    ).also { job ->
      job.expressionsOfInterest.forEach { (_, eoi) -> eoi.job = job }
      this.employer?.let { job.employer = this.employer }
    }
  }
}

private val mapper: ObjectMapper = jacksonObjectMapper()

internal fun String.asJson(): String {
  return mapper.writeValueAsString(this)
}

internal fun List<Int>.asStringList() = joinToString(separator = ",", prefix = "[", postfix = "]")

internal fun Job.mnJob(employerExtId: Long = 1L) = MNJob(
  jobTitle = title,
  jobDescription = description,
  postingDate = startDate?.toString(),
  closingDate = closingDate?.toString(),
  jobTypeId = 1,
  charityId = null,
  excludingOffences = MNExcludingOffences(
    choiceIds = offenceExclusions.split(",").map { offenceExclusionIdMap[it.uppercase()]!! },
    other = offenceExclusionsDetails,
  ),
  employerId = employerExtId,
  jobSourceOneId = jobSourceIdMap[sourcePrimary]!!,
  jobSourceTwoList = sourceSecondary?.let { listOf(jobSourceIdMap[it]!!) },
  employerSectorId = employerSectorIdMap[industrySector]!!,
  workPatternId = workPatternIdMap[workPattern]!!,
  contractTypeId = contractTypeIdMap[contractType]!!,
  hoursId = hoursPerWeekIdMap[hoursPerWeek]!!,
  rollingOpportunity = isRollingOpportunity,
  baseLocationId = baseLocationIdMap[baseLocation]!!,
  postcode = postcode,
  salaryFrom = salaryFrom.toString(),
  salaryTo = salaryTo?.toString(),
  salaryPeriodId = salaryPeriodIdMap[salaryPeriod]!!,
  additionalSalaryInformation = additionalSalaryInformation,
  nationalMinimumWage = isPayingAtLeastNationalMinimumWage,
  ringfencedJob = (if (!isRollingOpportunity) false else null),
  desirableJobCriteria = desirableCriteria,
  essentialJobCriteria = essentialCriteria,
  howToApply = howToApply,
)
