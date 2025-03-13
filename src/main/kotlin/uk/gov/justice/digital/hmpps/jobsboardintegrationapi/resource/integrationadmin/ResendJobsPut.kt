package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.resource.integrationadmin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.JobRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.ResendJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ResendDataResponse
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiDocRequestBody

@RestController
@RequestMapping("/integration-admin/resend-jobs")
class ResendJobsPut(
  private val registrar: JobRegistrar?,
) {

  @PutMapping("")
  @Tag(name = "Resend data")
  @Operation(
    summary = "Resend current jobs",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully resend job(s)",
        content = [
          Content(
            schema = Schema(implementation = ResendDataResponse::class),
            examples = [
              ExampleObject(
                value = """
                {
                  "itemCount": 4,
                  "totalCount": 397
                }
                """,
              ),
            ],
          ),
        ],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorised", content = [Content()]),
    ],
  )
  fun resend(
    @ApiDocRequestBody(
      content = [
        Content(
          schema = Schema(
            implementation = ResendJobRequest::class,
            examples = [
              """
                {
                  "jobIds": ["cdffb345-9961-4fa8-889c-040a37814052", "6dcd142c-5312-4bf1-b445-62bd20822d26"],
                  "forceUpdate": true
                }
                """,
              """{"jobIds": ["cdffb345-9961-4fa8-889c-040a37814052", "6dcd142c-5312-4bf1-b445-62bd20822d26"]}""",
            ],
          ),
        ),
      ],
    ) @RequestBody resendJobRequest: ResendJobRequest?,
  ) = registrar?.let {
    var itemCount: Long
    var totalCount: Long
    if (resendJobRequest?.jobIds.isNullOrEmpty()) {
      registrar.discoverAndResend().also {
        itemCount = it.first
        totalCount = it.second
      }
    } else {
      with(resendJobRequest!!) {
        itemCount = registrar.resend(jobIds!!, forceUpdate).toLong()
        totalCount = jobIds.size.toLong()
      }
    }

    ResendDataResponse(itemCount, totalCount)
  } ?: run { throw Exception("Registrar not initialised!") }
}
