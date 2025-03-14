package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateJobResponse

interface MNJobBoardApiClient {
  fun createEmployer(request: CreateEmployerRequest): CreateEmployerResponse

  fun updateEmployer(request: UpdateEmployerRequest): UpdateEmployerResponse

  fun createJob(request: CreateJobRequest): CreateJobResponse

  fun updateJob(request: UpdateJobRequest): UpdateJobResponse
}
