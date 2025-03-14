package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.ExpressionOfInterest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetEmployersResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.GetJobsResponse

interface JobsBoardApiClient {
  fun getEmployer(id: String): Employer?

  fun getAllEmployers() = getAllEmployers(DEFAULT_PAGE, FETCH_SIZE)
  fun getAllEmployers(page: Int, pageSize: Int): GetEmployersResponse

  fun getJob(id: String): Job?
  fun getAllJobs() = getAllJobs(DEFAULT_PAGE, FETCH_SIZE)
  fun getAllJobs(page: Int, pageSize: Int): GetJobsResponse

  fun createExpressionOfInterest(expressionOfInterest: ExpressionOfInterest)

  companion object {
    const val FETCH_SIZE = 50
    const val DEFAULT_PAGE = 0
  }
}
