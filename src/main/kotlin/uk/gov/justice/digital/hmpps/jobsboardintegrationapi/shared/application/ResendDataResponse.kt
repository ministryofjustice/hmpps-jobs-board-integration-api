package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import io.swagger.v3.oas.annotations.media.Schema

data class ResendDataResponse(
  @Schema(description = "Count of item sent")
  val itemCount: Long,
  @Schema(description = "Count of total items")
  val totalCount: Long,
)
