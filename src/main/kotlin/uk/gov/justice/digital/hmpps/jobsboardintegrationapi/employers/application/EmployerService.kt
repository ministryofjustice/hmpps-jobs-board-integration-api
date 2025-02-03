package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EmployerSector
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EmployerStatus
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest

@ConditionalOnIntegrationEnabled
@Service
class EmployerService(
  private val jobsBoardApiClient: JobsBoardApiClient,
  private val mnJobBoardApiClient: MNJobBoardApiClient,
  private val employerExternalIdRepository: EmployerExternalIdRepository,
  private val refDataMappingRepository: RefDataMappingRepository,
) {
  fun retrieveById(id: String): Employer? = jobsBoardApiClient.getEmployer(id)

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

  private fun convertAndMapId(employer: Employer, id: Long? = null) = employer.run {
    MNEmployer(
      id = id,
      employerName = name,
      employerBio = description,
      sectorId = translateId(EmployerSector, sector),
      partnerId = translateId(EmployerStatus, status),
    )
  }

  private fun retrieveExternalIdById(id: String): Long? = employerExternalIdRepository.findByKeyId(id)?.key?.externalId

  private fun translateId(refData: RefData, value: String) =
    refDataMappingRepository.findByDataRefDataAndDataValue(refData.type, value)?.data?.externalId ?: run {
      throw IllegalArgumentException("Reference data does not exist! refData=${refData.type}: value=$value")
    }
}
