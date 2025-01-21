package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient

@Service
class EmployerService(
  private val jobsBoardApiClient: JobsBoardApiClient,
) {
  fun retrieveById(id: String): Employer? = jobsBoardApiClient.getEmployer(id)
}
