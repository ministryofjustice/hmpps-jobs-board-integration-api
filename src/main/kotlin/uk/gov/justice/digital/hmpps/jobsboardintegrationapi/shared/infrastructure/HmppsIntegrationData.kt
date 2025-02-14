package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import com.fasterxml.jackson.annotation.JsonValue

data class HmppsMessage(
  val messageId: String,
  val eventType: HmppsMessageEventType,
  val description: String? = null,
  val messageAttributes: Map<String, String> = emptyMap(),
)

enum class HmppsMessageEventType(
  val type: String,
  @JsonValue val eventTypeCode: String,
  val description: String,
) {
  EXPRESSION_OF_INTEREST_CREATED(
    type = "mjma-jobs-board.job.expression-of-interest.created",
    eventTypeCode = "ExpressionOfInterestCreated",
    description = "Expression-of-Interest to the job has been given by the prisoner",
  ),
  ;

  companion object {
    private val typeToValue by lazy { entries.associateBy(HmppsMessageEventType::type) }

    fun fromType(type: String) = typeToValue[type]
  }
}
