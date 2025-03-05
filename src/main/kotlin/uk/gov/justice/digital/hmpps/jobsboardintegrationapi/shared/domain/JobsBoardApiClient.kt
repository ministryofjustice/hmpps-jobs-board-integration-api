package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.ExpressionOfInterest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse

interface JobsBoardApiClient {
  fun getEmployer(id: String): Employer?

  fun getAllEmployers() = getAllEmployers(0, FETCH_SIZE)
  fun getAllEmployers(page: Int, pageSize: Int): GetEmployersResponse

  fun getJob(id: String): Job?

  fun createExpressionOfInterest(expressionOfInterest: ExpressionOfInterest)

  companion object {
    const val FETCH_SIZE = 50
  }
}
