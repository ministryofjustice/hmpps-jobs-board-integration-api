package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.mockito.Mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.employerSectorIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.employerStatusIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.baseLocationIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.contractTypeIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.hoursPerWeekIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.jobSourceIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.offenceExclusionIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.salaryPeriodIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.workPatternIdMap
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.BASE_LOCATION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.CONTRACT_TYPE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_STATUS
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.HOURS_PER_WEEK
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.JOB_SOURCE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.OFFENCE_EXCLUSION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.SALARY_PERIOD
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.WORK_PATTERN
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
  protected lateinit var jobExternalIdRepository: JobExternalIdRepository

  @Mock
  protected lateinit var refDataMappingRepository: RefDataMappingRepository

  private val refDataMappingsMap = mapOf(
    EMPLOYER_STATUS.type to employerStatusIdMap,
    EMPLOYER_SECTOR.type to employerSectorIdMap,
    JOB_SOURCE.type to jobSourceIdMap,
    OFFENCE_EXCLUSION.type to offenceExclusionIdMap,
    WORK_PATTERN.type to workPatternIdMap,
    CONTRACT_TYPE.type to contractTypeIdMap,
    HOURS_PER_WEEK.type to hoursPerWeekIdMap,
    BASE_LOCATION.type to baseLocationIdMap,
    SALARY_PERIOD.type to salaryPeriodIdMap,
  )

  protected fun givenRefDataIdMapping(refData: String, dataValue: String) {
    whenever(refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(refData, dataValue)).thenAnswer {
      refDataMappingsMap[refData]?.get(dataValue)?.let { RefDataMapping(refData, dataValue, it) }
    }
  }
}
