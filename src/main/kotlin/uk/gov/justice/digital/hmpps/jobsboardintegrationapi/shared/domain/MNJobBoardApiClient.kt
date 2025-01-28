package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreatEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreatEmployerResponse

interface MNJobBoardApiClient {
  fun createEmployer(request: CreatEmployerRequest): CreatEmployerResponse
}
