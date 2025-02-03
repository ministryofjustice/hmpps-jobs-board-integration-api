package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateEmployerResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateEmployerResponse

interface MNJobBoardApiClient {
  fun createEmployer(request: CreateEmployerRequest): CreateEmployerResponse

  fun updateEmployer(request: UpdateEmployerRequest): UpdateEmployerResponse
}
