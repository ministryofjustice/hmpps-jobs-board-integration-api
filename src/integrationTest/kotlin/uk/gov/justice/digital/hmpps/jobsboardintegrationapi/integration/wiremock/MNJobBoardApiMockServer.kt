package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asJson
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.asStringList
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob

private const val EMPLOYERS_ENDPOINT = "/employers"
private const val JOBS_ENDPOINT = "/jobs-prison-leavers"

class MNJobBoardApiMockServer : WireMockServer(8093) {
  private var nextEmployerId: Long = 1L
  private var nextJobId: Long = 1L

  companion object {
    const val SCENARIO_CREATE_EMPLOYER = "CreateEmployer"
    const val SCENARIO_CREATE_JOB = "CreateJob"

    private const val PATH_ID = "id"
    private const val EMPLOYER_PATH_TEMPLATE = "$EMPLOYERS_ENDPOINT/{$PATH_ID}"
  }

  fun stubCreateEmployer(mnEmployer: MNEmployer) = stubPostWithReply(
    url = EMPLOYERS_ENDPOINT,
    replyBody = mnEmployer.copy(id = nextEmployerId++).createdResponse(),
  )

  fun resetStates(nextEmployerId: Long? = null, nextJobId: Long? = null) {
    this.nextEmployerId = nextEmployerId ?: 1L
    this.nextJobId = nextJobId ?: 1L
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

  fun stubUpdateEmployer(employerExternalIds: List<Long>) = employerExternalIds.forEach { id ->
    stubPostWithReplyTemplate(EMPLOYER_PATH_TEMPLATE, id.toString(), mnEmployerUpdatedResponseTemplate)
  }

  fun stubCreateEmployerUnauthorised() = stubPostUnauthorised(EMPLOYERS_ENDPOINT)

  fun stubUpdateEmployerUnauthorised(id: Long) = stubPostUnauthorised("$EMPLOYERS_ENDPOINT/$id")

  fun stubCreateJob(mnJob: MNJob, newId: Long) = stubPostWithReply(JOBS_ENDPOINT, mnJob.copy(id = newId).createdResponse())

  fun stubCreateJob(jobs: List<Job>) {
    var state = Scenario.STARTED
    jobs.forEach {
      val id = nextJobId++
      val nextState = nextJobId.toString()

      stubStatefulPostWithReply(
        url = JOBS_ENDPOINT,
        replyBody = dummyJobCreatedResponse(id),
        scenario = SCENARIO_CREATE_JOB,
        state = state,
        nextState = nextState,
      )
      state = nextState
    }
  }

  fun stubCreateJobUnauthorised() = stubPostUnauthorised(JOBS_ENDPOINT)

  fun stubUpdateJob(mnJob: MNJob) {
    requireNotNull(mnJob.id) { "Job external ID is missing" }
    stubPutWithReply(JOBS_ENDPOINT, mnJob.updatedResponse())
  }

  fun stubUpdateJob() = stubPutWithReplyTemplate(JOBS_ENDPOINT, mnJobUpdatedResponseTemplate)

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

  private fun stubPostWithReplyTemplate(pathTemplate: String, pathId: String, replyTemplate: String, statusCode: Int = 201) {
    stubFor(
      post(urlPathTemplate(pathTemplate))
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .withPathParam(PATH_ID, equalTo(pathId))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(statusCode)
            .withBody(replyTemplate)
            .withTransformers("response-template"),
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

  private fun stubPutWithReplyTemplate(url: String, replyTemplate: String) {
    stubFor(
      put(url)
        .withHeader("Authorization", matching("^Bearer .+\$"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(replyTemplate)
            .withTransformers("response-template"),
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

private fun dummyJobCreatedResponse(id: Long) = """
  {
    "message": {
    "successCode": "J2031",
    "successMessage": "Successfully added jobs for prison leavers",
    "httpStatusCode": 201
  },
    "responseObject": {
    "id": $id,
    "jobTitle": "dummy",
    "jobDescription": "dummy",
    "postingDate": null,
    "closingDate": null,
    "jobTypeId": 0,
    "charityId": null,
    "excludingOffences": {
      "choiceIds": [],
      "other": null
    },
    "employerId": 0,
    "jobSourceOneId": 0,
    "jobSourceTwoList": [],
    "employerSectorId": 0,
    "workPatternId": 0,
    "contractTypeId": 0,
    "hoursId": 0,
    "rollingOpportunity": false,
    "baseLocationId": 0,
    "postcode": null,
    "salaryFrom": "0",
    "salaryTo": null,
    "salaryPeriodId": 0,
    "additionalSalaryInformation": null,
    "nationalMinimumWage": false,
    "ringfencedJob": false,
    "desireableJobCriteria": null,
    "essentialJobCriteria": "dummy",
    "howToApply": "dummy"
  }
}
""".trimIndent()

private fun MNJob.createdResponse() = makeMNJobResponse(this, true)
private fun MNJob.updatedResponse() = makeMNJobResponse(this, false)

private fun makeMNJobResponse(mnJob: MNJob, created: Boolean = true) = mnJob.run {
  """
    {
      "message": {
          "successCode": "${if (created) "J2031" else "J2032"}",
          "successMessage": "Successfully ${if (created) "added" else "updated"} jobs for prison leavers",
          "httpStatusCode": ${if (created) 201 else 200}
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
}

private val mnEmployerUpdatedResponseTemplate = """
  {
    "message": {
        "successCode": "J2048",
        "successMessage": "Successfully updated employer",
        "httpStatusCode": 200
    },
    "responseObject": {
        "id": "{{jsonPath request.body '${'$'}.id'}}",
        "employerName": "dummy",
        "employerBio": "dummy",
        "sectorId": 1,
        "partnerId": 1,
        "imgName": null,
        "path": null
    }
  }
""".trimIndent()

private val mnJobUpdatedResponseTemplate = """
  {
    "message": {
        "successCode": "J2032",
        "successMessage": "Successfully updated jobs for prison leavers",
        "httpStatusCode": 200
    },
    "responseObject": {
        "id": "{{jsonPath request.body '$.id'}}",
        "jobTitle": "dummy",
        "jobDescription": "dummy",
        "postingDate": null,
        "closingDate": null,
        "jobTypeId": 0,
        "charityId": null,
        "excludingOffences": {
          "choiceIds": [],
          "other": null
        },
        "employerId": 0,
        "jobSourceOneId": 0,
        "jobSourceTwoList": [],
        "employerSectorId": 0,
        "workPatternId": 0,
        "contractTypeId": 0,
        "hoursId": 0,
        "rollingOpportunity": false,
        "baseLocationId": 0,
        "postcode": null,
        "salaryFrom": "0",
        "salaryTo": null,
        "salaryPeriodId": 0,
        "additionalSalaryInformation": null,
        "nationalMinimumWage": false,
        "ringfencedJob": false,
        "desireableJobCriteria": null,
        "essentialJobCriteria": "dummy",
        "howToApply": "dummy"
      }
  }
""".trimIndent()
