package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType.JOB_CREATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType.JOB_UPDATED
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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UUIDGenerator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventEmitter
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.TimeProvider
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobsResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNExcludingOffences
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateJobRequest

const val FETCH_SIZE = 50

@ConditionalOnIntegrationEnabled
@Service
class JobRetriever(
  private val jobsBoardApiClient: JobsBoardApiClient,
) {
  fun retrieve(id: String): Job = jobsBoardApiClient.getJob(id) ?: run {
    throw IllegalArgumentException("Job id=$id not found")
  }
}

@ConditionalOnIntegrationEnabled
@Service
class JobRegistrar(
  private val jobsBoardApiClient: JobsBoardApiClient,
  private val mnJobBoardApiClient: MNJobBoardApiClient,
  private val jobExternalIdRepository: JobExternalIdRepository,
  private val employerExternalIdRepository: EmployerExternalIdRepository,
  private val refDataMappingRepository: RefDataMappingRepository,
  private val eventEmitter: EventEmitter,
  private val uuidGenerator: UUIDGenerator,
  private val timeProvider: TimeProvider,
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
        assert(mnJob.id != null) { "MN Job ID is missing! jobId=${job.id}, jobTitle=${job.title}" }
        createIdMapping(mnJob.id!!, job.id)
      } catch (throwable: Throwable) {
        "Fail to register job-creation; jobId=${job.id}, jobTitle=${job.title}".let { message ->
          throw Exception(message, throwable)
        }
      }
    }
  }

  fun registerUpdate(job: Job) {
    try {
      val pendingUpdate = convertExisting(job)
      val updated = update(pendingUpdate)
      assert(updated.id == pendingUpdate.id) {
        "MN Job ID has changed! jobId=${job.id}, jobTitle=${job.title}; previous ID=${pendingUpdate.id}, new ID=${updated.id}"
      }
    } catch (throwable: Throwable) {
      "Fail to register job-update; jobId=${job.id}, jobTitle=${job.title}".let { message ->
        throw Exception(message, throwable)
      }
    }
  }

  /**
   * @return sentCount, totalCount
   */
  fun discoverAndResend(fetchSize: Int = FETCH_SIZE): Pair<Long, Long> {
    log.info("Resending jobs|started")
    var totalSentCount = 0L
    var currentPage: GetJobsResponse? = null
    do {
      log.info("Resending jobs|discovering")
      val pageNumber = currentPage?.let { it.page.number + 1 } ?: 0
      currentPage = retrieveAll(pageNumber, fetchSize)
      log.info("Resending jobs|sending page {}", currentPage.page.number)
      totalSentCount += doResend(currentPage.content.map { it.id })
    } while (currentPage!!.hasNext())

    log.info("Resending jobs|completed: itemCount={}, totalCount={}", totalSentCount, 0)
    return Pair(totalSentCount, currentPage.page.totalElements)
  }

  /**
   * @return sentCount
   */
  fun resend(jobIds: List<String>, forceUpdate: Boolean = false, fetchSize: Int = FETCH_SIZE): Int {
    log.info("Resending jobs|started")
    var totalSentCount = 0

    jobIds.chunked(fetchSize).forEachIndexed { index, chunk ->
      log.info("Resending jobs|sending chunk {}", index)
      log.info("Resending jobs|chunk = {}", chunk)
      totalSentCount += doResend(chunk, forceUpdate)
    }

    log.info("Resending jobs|completed: itemCount={}, totalCount={}", totalSentCount, 0)
    return totalSentCount
  }

  private fun retrieveAll(page: Int, pageSize: Int): GetJobsResponse = jobsBoardApiClient.getAllJobs(page, pageSize)

  private fun create(mnJob: MNJob): MNJob {
    val request = CreateJobRequest.from(mnJob)
    return mnJobBoardApiClient.createJob(request)
  }

  private fun update(mnJob: MNJob): MNJob {
    val request = UpdateJobRequest.from(mnJob)
    return mnJobBoardApiClient.updateJob(request)
  }

  private fun convert(newJob: Job) = convertAndMapId(newJob, retrieveEmployerExternalIdById(newJob.employerId))

  private fun convertExisting(existingJob: Job): MNJob {
    val extId = retrieveExternalIdById(existingJob.id)
    return if (extId != null) {
      convertAndMapId(existingJob, retrieveEmployerExternalIdById(existingJob.employerId), extId)
    } else {
      throw IllegalStateException("Job with id=${existingJob.id} not found (ID mapping missing)")
    }
  }

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
      postingDate = null,
      closingDate = closingDate?.toString(),
      jobTypeId = 1,
      charityId = null,
      excludingOffences = MNExcludingOffences(
        choiceIds = offenceExclusions.split(",").map { translateOptionalId(OFFENCE_EXCLUSION, it.uppercase()) }
          .filterNotNull(),
        other = offenceExclusionsDetails,
      ),
      jobSourceOneId = translateId(JOB_SOURCE, sourcePrimary),
      jobSourceTwoList = sourceSecondary.let {
        if (!it.isNullOrEmpty()) listOf(translateOptionalId(JOB_SOURCE, it)!!) else null
      },
      employerSectorId = translateId(EMPLOYER_SECTOR, industrySector),
      workPatternId = translateId(WORK_PATTERN, workPattern),
      contractTypeId = translateId(CONTRACT_TYPE, contractType),
      hoursId = translateId(HOURS_PER_WEEK, hoursPerWeek),
      rollingOpportunity = isRollingOpportunity,
      baseLocationId = baseLocation?.let { translateOptionalId(BASE_LOCATION, it) },
      postcode = postcode,
      salaryFrom = salaryFrom.toString(),
      salaryTo = salaryTo?.toString(),
      salaryPeriodId = translateId(SALARY_PERIOD, salaryPeriod),
      additionalSalaryInformation = additionalSalaryInformation,
      nationalMinimumWage = isPayingAtLeastNationalMinimumWage,
      ringfencedJob = isOnlyForPrisonLeavers,
      desirableJobCriteria = desirableCriteria,
      essentialJobCriteria = essentialCriteria,
      howToApply = howToApply,
    )
  }

  private fun retrieveExternalIdById(id: String): Long? = jobExternalIdRepository.findByKeyId(id)?.key?.externalId

  private fun retrieveEmployerExternalIdById(employerId: String): Long = employerExternalIdRepository.findByKeyId(employerId)?.key?.externalId.also {
    assert(it != null) { "Employer external ID is not found for employerId=$employerId" }
  }!!

  private fun translateOptionalId(refData: RefData, value: String?) = value?.let { if (it.isNotEmpty()) translateId(refData, it) else null }

  private fun translateId(refData: RefData, value: String) = refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(refData.type, value)?.data?.externalId ?: run {
    throw IllegalArgumentException("Reference data does not exist! refData=${refData.type}: value=$value")
  }

  private fun doResend(ids: List<String>, forceUpdate: Boolean = false): Int = when {
    ids.isEmpty() -> 0
    !forceUpdate -> ids.filter { id -> !existsIdMappingById(id) }.toList().let { sendEvents(JOB_CREATED, it) }
    else -> ids.groupBy { id -> !existsIdMappingById(id) }.filterValues { it.isNotEmpty() }.map {
      val type = if (it.key) JOB_CREATED else JOB_UPDATED
      sendEvents(type, it.value)
    }.sum()
  }

  private fun sendEvents(eventType: JobEventType, ids: List<String>): Int {
    var totalSentCount = 0
    if (ids.isNotEmpty()) {
      log.debug("Resending jobs|sending with these IDs: {}", ids)
      val jobIdsToSent = if (eventType == JOB_CREATED) excludePastJobs(ids) else ids
      jobIdsToSent.map { id -> makeEventForJob(id, eventType) }.toTypedArray().let { events ->
        log.debug("Sending events {}", events)
        sendEvent(*events).let { sentCount -> totalSentCount += sentCount }
      }
    }
    return totalSentCount
  }

  /**
   * Exclude past jobs: only include rolling job or otherwise with closing date after today
   */
  private fun excludePastJobs(ids: List<String>) = runBlocking {
    val today = timeProvider.now().toLocalDate()
    ids.map { id -> async(Dispatchers.IO) { jobsBoardApiClient.getJob(id) } }.awaitAll()
      .filterNotNull().filter { it.isRollingOpportunity || (it.closingDate?.isAfter(today) ?: true) }
      .map { it.id }
  }.also { log.debug("Resending jobs|sending with these (filtered) IDs: {}", it) }

  private fun makeEventForJob(
    jobId: String,
    jobEventType: JobEventType,
  ) = JobEvent(
    eventId = uuidGenerator.generate(),
    eventType = jobEventType,
    timestamp = timeProvider.nowAsInstant(),
    jobId = jobId,
  )

  private fun sendEvent(vararg jobEvents: JobEvent): Int = runBlocking {
    val sendTasks = jobEvents.map {
      async(Dispatchers.IO) {
        try {
          eventEmitter.send(it.eventData())
          sendEventSuccess(it.jobId)
        } catch (ex: Throwable) {
          sendEventFailure(ex)
        }
      }
    }

    val results = sendTasks.awaitAll()
    val completedIds = results.mapNotNull { it.first }
    val errors = results.mapNotNull { it.second }
    if (errors.isNotEmpty()) {
      errors.groupingBy { it.message }.eachCount()
        .map { "Error: ${it.key}, count: ${it.value}" }.joinToString(separator = ";")
        .let { log.error("Error sending job events (thus skipped), total error count: {}, errors: {}", errors.size, it) }
    }
    log.trace("Completed job events with these job IDs: {}", completedIds)

    completedIds.size
  }

  private fun JobEvent.eventData() = EventData(
    eventId = eventId,
    eventType = eventType.type,
    timestamp = timestamp,
    content = """
       {
      "eventId": "$eventId",
      "eventType": "${eventType.eventTypeCode}",
      "timestamp": "$timestamp",
      "jobId": "$jobId"
      }
    """.trimIndent(),
  )
}

private fun sendEventSuccess(id: String) = SendEventResult(id, null)
private fun sendEventFailure(error: Throwable) = SendEventResult(null, error)

private typealias SendEventResult = Pair<String?, Throwable?>
