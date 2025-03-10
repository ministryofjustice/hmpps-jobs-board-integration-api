package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import io.swagger.v3.oas.annotations.media.Schema

data class ResendEmployerRequest(
  @Schema(description = "The identifiers of the employer (optional)")
  val employerIds: List<String>? = null,
  @Schema(description = "force updating the given employers (explicit employer IDs are required); No effect without the employer IDs; false by default")
  val forceUpdate: Boolean = false,
)
