package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.ExpressionOfInterest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job

interface JobsBoardApiClient {
  fun getEmployer(id: String): Employer?

  fun getJob(id: String): Job?

  fun createExpressionOfInterest(expressionOfInterest: ExpressionOfInterest)
}
