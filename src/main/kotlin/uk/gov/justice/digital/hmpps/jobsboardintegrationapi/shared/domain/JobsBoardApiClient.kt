package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

interface JobsBoardApiClient {
  fun getEmployer(id: String): Employer?
}
