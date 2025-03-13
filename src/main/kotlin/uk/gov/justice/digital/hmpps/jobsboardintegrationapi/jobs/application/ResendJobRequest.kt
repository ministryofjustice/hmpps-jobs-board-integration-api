package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import io.swagger.v3.oas.annotations.media.Schema

data class ResendJobRequest(
  @Schema(description = "The identifiers of the job (optional)")
  val jobIds: List<String>? = null,
  @Schema(description = "force updating the given jobs (explicit job IDs are required); No effect without the job IDs; false by default")
  val forceUpdate: Boolean = false,
)
