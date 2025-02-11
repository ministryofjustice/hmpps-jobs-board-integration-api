package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asJson
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asStringList
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob

private const val EMPLOYERS_ENDPOINT = "/employers"
private const val JOBS_ENDPOINT = "/jobs-prison-leavers"

class MNJobBoardApiMockServer : WireMockServer(8093) {
  fun stubCreateEmployer(mnEmployer: MNEmployer, newId: Long) =
    stubPostWithReply(EMPLOYERS_ENDPOINT, mnEmployer.copy(id = newId).response())

  fun stubUpdateEmployer(mnEmployer: MNEmployer) =
    stubPostWithReply("$EMPLOYERS_ENDPOINT/${mnEmployer.id!!}", mnEmployer.copy().response())

  fun stubCreateEmployerUnauthorised() = stubPostUnauthorised(EMPLOYERS_ENDPOINT)

  fun stubUpdateEmployerUnauthorised(id: Long) = stubPostUnauthorised("$EMPLOYERS_ENDPOINT/$id")

  fun stubCreateJob(mnJob: MNJob, newId: Long) = stubPostWithReply(JOBS_ENDPOINT, mnJob.copy(id = newId).response())

  fun stubCreateJobUnauthorised() = stubPostUnauthorised(JOBS_ENDPOINT)

  fun stubUpdateJob(mnJob: MNJob) {
    requireNotNull(mnJob.id) { "Job external ID is missing" }
    stubPutWithReply(JOBS_ENDPOINT, mnJob.response())
  }

  fun stubUpdateJobUnauthorised() = stubPutUnauthorised(JOBS_ENDPOINT)

  private fun stubPostWithReply(url: String, replyBody: String) {
    stubFor(
      post(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(replyBody),
        ),
    )
  }

  private fun stubPostUnauthorised(url: String) {
    stubFor(
      post(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withStatus(401),
        ),
    )
  }

  private fun stubPutWithReply(url: String, replyBody: String) {
    stubFor(
      put(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(replyBody),
        ),
    )
  }

  private fun stubPutUnauthorised(url: String) {
    stubFor(
      put(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withStatus(401),
        ),
    )
  }
}

class MNJobBoardApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val mnJobBoardApi = MNJobBoardApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = mnJobBoardApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = mnJobBoardApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = mnJobBoardApi.stop()
}

private fun MNEmployer.response() = """
  {
    "message": {
        "successCode": "J2047",
        "successMessage": "Successfully added employer",
        "httpStatusCode": 201
    },
    "responseObject": {
        "id": $id,
        "employerName": "$employerName",
        "employerBio": "$employerBio",
        "sectorId": $sectorId,
        "partnerId": $partnerId,
        "imgName": ${imgName?.let { "\"$it\"" }},
        "path": ${path?.let { "\"$it\"" }}
    }
  }
""".trimIndent()

private fun MNJob.response() = """
  {
    "message": {
        "successCode": "J2031",
        "successMessage": "Successfully added jobs for prison leavers",
        "httpStatusCode": 201
    },
    "responseObject": {
        "id": $id,
        "jobTitle": "$jobTitle",
        "jobDescription": ${jobDescription.asJson()},
        "postingDate": ${postingDate?.let { "\"$it\"" }},
        "closingDate": ${closingDate?.let { "\"$it\"" }},
        "jobTypeId": $jobTypeId,
        "charityId": $charityId,
        "excludingOffences": {
            "choiceIds": ${excludingOffences.choiceIds.asStringList()},
            "other": ${excludingOffences.other?.asJson()}
        },
        "employerId": $employerId,
        "jobSourceOneId": $jobSourceOneId,
        "jobSourceTwoList": ${jobSourceTwoList?.asStringList()},
        "employerSectorId": $employerSectorId,
        "workPatternId": $workPatternId,
        "contractTypeId": $contractTypeId,
        "hoursId": $hoursId,
        "rollingOpportunity": $rollingOpportunity,
        "baseLocationId": $baseLocationId,
        "postcode": ${postcode?.let { "\"$it\"" }},
        "salaryFrom": "$salaryFrom",
        "salaryTo": ${salaryTo?.let { "\"$it\"" }},
        "salaryPeriodId": $salaryPeriodId,
        "additionalSalaryInformation": ${additionalSalaryInformation?.asJson()},
        "nationalMinimumWage": $nationalMinimumWage,
        "ringfencedJob": $ringfencedJob,
        "desireableJobCriteria": ${desirableJobCriteria?.asJson()},
        "essentialJobCriteria": ${essentialJobCriteria.asJson()},
        "howToApply": ${howToApply.asJson()}
    }
  }
""".trimIndent()
