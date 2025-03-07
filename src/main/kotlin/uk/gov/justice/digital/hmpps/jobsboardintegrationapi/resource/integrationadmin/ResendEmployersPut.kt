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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.ResendEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ResendDataResponse
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiDocRequestBody

@RestController
@RequestMapping("/integration-admin/resend-employers")
class ResendEmployersPut(
  private val registrar: EmployerRegistrar?,
) {

  @PutMapping("")
  @Tag(name = "Resend data")
  @Operation(
    summary = "Resend current employers",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully resend employer(s)",
        content = [
          Content(
            schema = Schema(implementation = ResendDataResponse::class),
            examples = [
              ExampleObject(
                value = """
                {
                  "itemCount": 4,
                  "totalCount": 97
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
            implementation = ResendEmployerRequest::class,
            examples = [
              """
                {
                  "employerIds": ["5a8aba39-b562-43f8-b31b-4da64b612b31", "c61a1879-6c70-438f-bc36-5fa5d648811b"],
                  "forceUpdate": true
                }
                """,
              """{"employerIds": ["5a8aba39-b562-43f8-b31b-4da64b612b31", "c61a1879-6c70-438f-bc36-5fa5d648811b"]}""",
            ],
          ),
        ),
      ],
    ) @RequestBody resendEmployerRequest: ResendEmployerRequest?,
  ) = registrar?.let {
    var itemCount: Long
    var totalCount: Long
    if (resendEmployerRequest?.employerIds.isNullOrEmpty()) {
      registrar.discoverAndResend().also {
        itemCount = it.first
        totalCount = it.second
      }
    } else {
      with(resendEmployerRequest!!) {
        itemCount = registrar.resend(employerIds!!, forceUpdate).toLong()
        totalCount = employerIds.size.toLong()
      }
    }

    ResendDataResponse(itemCount, totalCount)
  } ?: run { throw Exception("Registrar not initialised!") }
}
