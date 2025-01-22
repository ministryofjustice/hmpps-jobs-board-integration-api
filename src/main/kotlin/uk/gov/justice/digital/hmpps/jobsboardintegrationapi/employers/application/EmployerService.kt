package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.MNJobBoardApiClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreatEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer

@ConditionalOnIntegrationEnabled
@Service
class EmployerService(
  private val jobsBoardApiClient: JobsBoardApiClient,
  private val mnJobBoardApiClient: MNJobBoardApiClient,
) {
  fun retrieveById(id: String): Employer? = jobsBoardApiClient.getEmployer(id)

  fun create(mnEmployer: MNEmployer): MNEmployer {
    val request = CreatEmployerRequest.from(mnEmployer)
    // TODO cater optional fields for employerStatus = KEY_PARTNER (sectorId==2)
    return mnJobBoardApiClient.createEmployer(request)
  }

  fun convert(employer: Employer) = employer.run {
    MNEmployer(
      employerName = name,
      employerBio = description,
      // FIXME translate from employer.sector
      sectorId = 1,
      // FIXME translate from employer.status
      partnerId = 1,
    )
  }

  fun createIdMapping(mnId: Long, employerId: String) {
    // TODO persist ID mapping
    throw NotImplementedError("ID Mapping is not yet implemented")
  }
}
