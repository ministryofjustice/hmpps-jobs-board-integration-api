package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_CREATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType.EMPLOYER_UPDATED
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UUIDGenerator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.TimeProvider
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse

const val FETCH_SIZE = 50

@ConditionalOnIntegrationEnabled
@Service
class EmployerRegistrar(
  private val employerService: EmployerService,
  private val uuidGenerator: UUIDGenerator,
  private val timeProvider: TimeProvider,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun registerCreation(employer: Employer) {
    if (employerService.existsIdMappingById(employer.id)) {
      log.warn("Employer with id={} already exists (with ID mapping), thus skipping", employer.id)
    } else {
      try {
        val mnEmployer = employerService.run { create(convert(employer)) }
        assert(mnEmployer.id != null) { "MN Employer ID is missing! employerId=${employer.id}, employerName=${employer.name}" }
        employerService.createIdMapping(mnEmployer.id!!, employer.id)
      } catch (throwable: Throwable) {
        "Fail to register employer-creation; employerId=${employer.id}, employerName=${employer.name}".let { message ->
          throw Exception(message, throwable)
        }
      }
    }
  }

  fun registerUpdate(employer: Employer) {
    try {
      val pendingUpdate = employerService.convertExisting(employer)
      val updated = employerService.update(pendingUpdate)
      assert(updated.id == pendingUpdate.id) {
        "MN Employer ID has changed! employerId=${employer.id}, employerName=${employer.name}; previous ID=${pendingUpdate.id}, new ID=${updated.id}"
      }
    } catch (throwable: Throwable) {
      "Fail to register employer-update; employerId=${employer.id}, employerName=${employer.name}".let { message ->
        throw Exception(message, throwable)
      }
    }
  }

  /**
   * @return sentCount, totalCount
   */
  fun discoverAndResend(fetchSize: Int = FETCH_SIZE): Pair<Long, Long> {
    log.info("Resending employers|started")
    var totalSentCount = 0L
    var currentPage: GetEmployersResponse? = null
    do {
      log.info("Resending employers|discovering")
      val pageNumber = currentPage?.let { it.page.number + 1 } ?: 0
      currentPage = employerService.retrieveAll(pageNumber, fetchSize)
      log.info("Resending employers|sending page {}", currentPage.page.number)
      totalSentCount += doResend(currentPage.content.map { it.id })
    } while (currentPage!!.hasNext())

    log.info("Resending employers|completed: itemCount={}, totalCount={}", totalSentCount, 0)
    return Pair(totalSentCount, currentPage.page.totalElements)
  }

  /**
   * @return sentCount
   */
  fun resend(employerIds: List<String>, forceUpdate: Boolean = false, fetchSize: Int = FETCH_SIZE): Int {
    log.info("Resending employers|started")
    var totalSentCount = 0

    employerIds.chunked(fetchSize).forEachIndexed { index, chunk ->
      log.info("Resending employers|sending chunk {}", index)
      log.info("Resending employers|chunk = {}", chunk)
      totalSentCount += doResend(chunk, forceUpdate)
    }

    log.info("Resending employers|completed: itemCount={}, totalCount={}", totalSentCount, 0)
    return totalSentCount
  }

  private fun doResend(ids: List<String>, forceUpdate: Boolean = false): Int = when {
    ids.isEmpty() -> 0
    !forceUpdate -> ids.filter { id -> !employerService.existsIdMappingById(id) }.toList().let { sendEvents(EMPLOYER_CREATED, it) }
    else ->
      ids.groupBy { id -> !employerService.existsIdMappingById(id) }.filterValues { it.isNotEmpty() }.map {
        val type = if (it.key) EMPLOYER_CREATED else EMPLOYER_UPDATED
        sendEvents(type, it.value)
      }.sum()
  }

  private fun sendEvents(eventType: EmployerEventType, ids: List<String>): Int {
    var totalSentCount = 0
    if (ids.isNotEmpty()) {
      log.debug("Resending employers|sending with these IDs: {}", ids)
      ids.map { id -> makeEventForEmployer(id, eventType) }.toTypedArray().let { events ->
        log.debug("Sending events {}", events)
        employerService.sendEvent(*events).let { sentCount -> totalSentCount += sentCount }
      }
    }
    return totalSentCount
  }

  private fun makeEventForEmployer(
    employerId: String,
    employerEventType: EmployerEventType,
  ): EmployerEvent = EmployerEvent(
    eventId = uuidGenerator.generate(),
    eventType = employerEventType,
    timestamp = timeProvider.nowAsInstant(),
    employerId = employerId,
  )
}
