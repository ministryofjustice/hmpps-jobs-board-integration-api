package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_STATUS
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventEmitter
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest

@ConditionalOnIntegrationEnabled
@Service
class EmployerService(
  private val jobsBoardApiClient: JobsBoardApiClient,
  private val mnJobBoardApiClient: MNJobBoardApiClient,
  private val employerExternalIdRepository: EmployerExternalIdRepository,
  private val refDataMappingRepository: RefDataMappingRepository,
  private val eventEmitter: EventEmitter,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  fun retrieveById(id: String): Employer? = jobsBoardApiClient.getEmployer(id)

  fun retrieveAll(page: Int, pageSize: Int): GetEmployersResponse = jobsBoardApiClient.getAllEmployers(page, pageSize)

  fun create(mnEmployer: MNEmployer): MNEmployer {
    val request = CreateEmployerRequest.from(mnEmployer)
    return mnJobBoardApiClient.createEmployer(request)
  }

  fun update(mnEmployer: MNEmployer): MNEmployer {
    val request = UpdateEmployerRequest.from(mnEmployer)
    return mnJobBoardApiClient.updateEmployer(request)
  }

  fun convert(newEmployer: Employer) = convertAndMapId(newEmployer)

  fun convertExisting(existingEmployer: Employer): MNEmployer {
    val extId = retrieveExternalIdById(existingEmployer.id)
    return if (extId != null) {
      convertAndMapId(existingEmployer, extId)
    } else {
      throw IllegalStateException("Employer with id=${existingEmployer.id} not found (ID mapping missing)")
    }
  }

  fun existsIdMappingById(id: String): Boolean = retrieveExternalIdById(id) != null

  fun createIdMapping(externalId: Long, id: String) {
    if (!existsIdMappingById(id)) {
      employerExternalIdRepository.save(EmployerExternalId(id, externalId))
    } else {
      throw Exception("Employer ID cannot be created! ID mapping already exists. ID pair: externalId=$externalId, id=$id")
    }
  }

  fun sendEvent(vararg employerEvents: EmployerEvent): Int = runBlocking {
    val sendTasks = employerEvents.map {
      async(Dispatchers.IO) {
        try {
          eventEmitter.send(it.eventData())
          sendEventSuccess(it.employerId)
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
        .let { log.error("Error sending employer events (thus skipped), total error count: {}, errors: {}", errors.size, it) }
    }
    log.trace("Completed employer events with these employer IDs: {}", completedIds)

    completedIds.size
  }

  private fun convertAndMapId(employer: Employer, id: Long? = null) = employer.run {
    MNEmployer(
      id = id,
      employerName = name,
      employerBio = description,
      sectorId = translateId(EMPLOYER_SECTOR, sector),
      partnerId = translateId(EMPLOYER_STATUS, status),
    )
  }

  private fun retrieveExternalIdById(id: String): Long? = employerExternalIdRepository.findByKeyId(id)?.key?.externalId

  private fun translateId(refData: RefData, value: String) = refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(refData.type, value)?.data?.externalId ?: run {
    throw IllegalArgumentException("Reference data does not exist! refData=${refData.type}: value=$value")
  }

  private fun EmployerEvent.eventData() = EventData(
    eventId = eventId,
    eventType = eventType.type,
    timestamp = timestamp,
    content = """
       {
      "eventId": "$eventId",
      "eventType": "${eventType.eventTypeCode}",
      "timestamp": "$timestamp",
      "employerId": "$employerId"
      }
    """.trimIndent(),
  )
}

private fun sendEventSuccess(id: String) = SendEventResult(id, null)
private fun sendEventFailure(error: Throwable) = SendEventResult(null, error)

private typealias SendEventResult = Pair<String?, Throwable?>
