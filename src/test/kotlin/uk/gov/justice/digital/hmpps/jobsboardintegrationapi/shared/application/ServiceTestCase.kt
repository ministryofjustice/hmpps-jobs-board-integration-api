package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.mockito.Mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sectorToIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.statusToPartnerIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EmployerSector
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EmployerStatus
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMapping
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient

abstract class ServiceTestCase : UnitTestBase() {
  @Mock
  protected lateinit var jobsBoardApiClient: JobsBoardApiClient

  @Mock
  protected lateinit var mnJobBoardApiClient: MNJobBoardApiClient

  @Mock
  protected lateinit var employerExternalIdRepository: EmployerExternalIdRepository

  @Mock
  protected lateinit var refDataMappingRepository: RefDataMappingRepository

  private val refDataMappingsMap = mapOf(
    EmployerStatus.type to statusToPartnerIdMap,
    EmployerSector.type to sectorToIdMap,
  )

  protected fun givenRefDataIdMapping(refData: String, dataValue: String) {
    whenever(refDataMappingRepository.findByDataRefDataAndDataValue(refData, dataValue)).thenAnswer {
      refDataMappingsMap[refData]?.get(dataValue)?.let { RefDataMapping(refData, dataValue, it) }
    }
  }
}
