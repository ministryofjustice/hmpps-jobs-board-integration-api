package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.BASE_LOCATION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.CONTRACT_TYPE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.HOURS_PER_WEEK
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.JOB_SOURCE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.OFFENCE_EXCLUSION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.SALARY_PERIOD
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.WORK_PATTERN
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNExcludingOffences
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob

@ConditionalOnIntegrationEnabled
@Service
class JobRetriever(
  private val jobsBoardApiClient: JobsBoardApiClient,
) {
  fun retrieve(id: String): Job {
    return jobsBoardApiClient.getJob(id) ?: run {
      throw IllegalArgumentException("Job id=$id not found")
    }
  }
}

@ConditionalOnIntegrationEnabled
@Service
class JobRegistrar(
  private val mnJobBoardApiClient: MNJobBoardApiClient,
  private val jobExternalIdRepository: JobExternalIdRepository,
  private val employerExternalIdRepository: EmployerExternalIdRepository,
  private val refDataMappingRepository: RefDataMappingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun registerCreation(job: Job) {
    if (existsIdMappingById(job.id)) {
      log.warn("Job with id={} already exists (with ID mapping), thus skipping", job.id)
    } else {
      try {
        val mnJob = create(convert(job))
        assert(mnJob.id != null) { "MN Job ID is missing! jobId=${job.id}, jobName=${job.title}" }
        createIdMapping(mnJob.id!!, job.id)
      } catch (throwable: Throwable) {
        "Fail to register job-creation; jobId=${job.id}, jobName=${job.title}".let { message ->
          throw Exception(message, throwable)
        }
      }
    }
  }

  private fun create(mnJob: MNJob): MNJob {
    val request = CreateJobRequest.from(mnJob)
    return mnJobBoardApiClient.createJob(request)
  }

  private fun convert(newJob: Job) = convertAndMapId(newJob, retrieveEmployerExternalIdById(newJob.employerId))

  private fun existsIdMappingById(id: String): Boolean = retrieveExternalIdById(id) != null

  private fun createIdMapping(externalId: Long, id: String) {
    assert(!existsIdMappingById(id)) { "Job ID cannot be created! ID mapping already exists. ID pair: externalId=$externalId, id=$id" }
    jobExternalIdRepository.save(JobExternalId(id, externalId))
  }

  private fun convertAndMapId(job: Job, employerExtId: Long, jobExtId: Long? = null) = job.run {
    MNJob(
      id = jobExtId,
      employerId = employerExtId,
      jobTitle = title,
      jobDescription = description,
      postingDate = startDate?.toString(),
      closingDate = closingDate?.toString(),
      jobTypeId = 1,
      charityId = null,
      excludingOffences = MNExcludingOffences(
        choiceIds = offenceExclusions.split(",").map { translateId(OFFENCE_EXCLUSION, it.uppercase()) },
        other = offenceExclusionsDetails,
      ),
      jobSourceOneId = translateId(JOB_SOURCE, sourcePrimary),
      jobSourceTwoList = sourceSecondary?.let { listOf(translateId(JOB_SOURCE, it)) },
      employerSectorId = translateId(EMPLOYER_SECTOR, industrySector),
      workPatternId = translateId(WORK_PATTERN, workPattern),
      contractTypeId = translateId(CONTRACT_TYPE, contractType),
      hoursId = translateId(HOURS_PER_WEEK, hoursPerWeek),
      rollingOpportunity = isRollingOpportunity,
      baseLocationId = baseLocation?.let { translateId(BASE_LOCATION, it) },
      postcode = postcode,
      salaryFrom = salaryFrom.toString(),
      salaryTo = salaryTo?.toString(),
      salaryPeriodId = translateId(SALARY_PERIOD, salaryPeriod),
      additionalSalaryInformation = additionalSalaryInformation,
      nationalMinimumWage = isPayingAtLeastNationalMinimumWage,
      ringfencedJob = (if (!isRollingOpportunity) false else null),
      desirableJobCriteria = desirableCriteria,
      essentialJobCriteria = essentialCriteria,
      howToApply = howToApply,
    )
  }

  private fun retrieveExternalIdById(id: String): Long? = jobExternalIdRepository.findByKeyId(id)?.key?.externalId

  private fun retrieveEmployerExternalIdById(employerId: String): Long =
    employerExternalIdRepository.findByKeyId(employerId)?.key?.externalId.also {
      assert(it != null) { "Employer external ID is not found for employerId=$employerId" }
    }!!

  private fun translateId(refData: RefData, value: String) =
    refDataMappingRepository.findByDataRefDataAndDataValue(refData.type, value)?.data?.externalId ?: run {
      throw IllegalArgumentException("Reference data does not exist! refData=${refData.type}: value=$value")
    }
}
