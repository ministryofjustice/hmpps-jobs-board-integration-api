package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asJson
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asStringList
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob

private const val EMPLOYERS_ENDPOINT = "/employers"
private const val JOBS_ENDPOINT = "/jobs-prison-leavers"

class MNJobBoardApiMockServer : WireMockServer(8093) {
  private var nextEmployerId: Long = 1L

  companion object {
    const val SCENARIO_CREATE_EMPLOYER = "CreateEmployer"
  }

  fun stubCreateEmployer(mnEmployer: MNEmployer) = stubPostWithReply(
    url = EMPLOYERS_ENDPOINT,
    replyBody = mnEmployer.copy(id = nextEmployerId++).createdResponse(),
  )

  fun resetStates(nextEmployerId: Long? = null) {
    this.nextEmployerId = nextEmployerId ?: 1L
  }

  fun stubCreateEmployer(employers: List<Employer>) {
    var state = Scenario.STARTED
    employers.forEach {
      val id = nextEmployerId++
      val nextState = nextEmployerId.toString()

      stubStatefulPostWithReply(
        url = EMPLOYERS_ENDPOINT,
        replyBody = dummyEmployerCreatedResponse(id),
        scenario = SCENARIO_CREATE_EMPLOYER,
        state = state,
        nextState = nextState,
      )
      state = nextState
    }
  }

  fun stubUpdateEmployer(mnEmployer: MNEmployer) = stubPostWithReply("$EMPLOYERS_ENDPOINT/${mnEmployer.id!!}", mnEmployer.copy().updatedResponse(), 200)

  fun stubCreateEmployerUnauthorised() = stubPostUnauthorised(EMPLOYERS_ENDPOINT)

  fun stubUpdateEmployerUnauthorised(id: Long) = stubPostUnauthorised("$EMPLOYERS_ENDPOINT/$id")

  fun stubCreateJob(mnJob: MNJob, newId: Long) = stubPostWithReply(JOBS_ENDPOINT, mnJob.copy(id = newId).response())

  fun stubCreateJobUnauthorised() = stubPostUnauthorised(JOBS_ENDPOINT)

  fun stubUpdateJob(mnJob: MNJob) {
    requireNotNull(mnJob.id) { "Job external ID is missing" }
    stubPutWithReply(JOBS_ENDPOINT, mnJob.response())
  }

  fun stubUpdateJobUnauthorised() = stubPutUnauthorised(JOBS_ENDPOINT)

  private fun stubPostWithReply(url: String, replyBody: String, statusCode: Int = 201) {
    stubFor(
      post(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(statusCode)
            .withBody(replyBody),
        ),
    )
  }

  private fun stubStatefulPostWithReply(url: String, replyBody: String, scenario: String, state: String, nextState: String) {
    stubFor(
      post(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .inScenario(scenario)
        .whenScenarioStateIs(state)
        .willSetStateTo(nextState)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
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

class MNJobBoardApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val mnJobBoardApi = MNJobBoardApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = mnJobBoardApi.start()
  override fun beforeEach(context: ExtensionContext) {
    mnJobBoardApi.resetAll()
    mnJobBoardApi.resetStates()
  }
  override fun afterAll(context: ExtensionContext): Unit = mnJobBoardApi.stop()
}

private fun dummyEmployerCreatedResponse(id: Long) = """
  {
    "message": {
        "successCode": "J2047",
        "successMessage": "Successfully added employer",
        "httpStatusCode": 201
    },
    "responseObject": {
        "id": $id,
        "employerName": "dummy",
        "employerBio": "dummy",
        "sectorId": 1,
        "partnerId": 1,
        "imgName": null,
        "path": null
    }
  }
""".trimIndent()

private fun MNEmployer.createdResponse() = """
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

private fun MNEmployer.updatedResponse() = """
  {
    "message": {
        "successCode": "J2048",
        "successMessage": "Successfully updated employer",
        "httpStatusCode": 200
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
