package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.resource.integrationadmin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension.Companion.jobsBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MNJobBoardApiExtension.Companion.mnJobBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MnAuthApiExtension.Companion.mnAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.abcConstructionApprentice
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import kotlin.test.Test

class ResendJobsPutShould : ResendDataTestCase() {
  companion object {
    private const val RESEND_JOBS_ENDPOINT = "/integration-admin/resend-jobs"
    private const val IDS_FIELD_NAME = "jobIds"
  }

  @BeforeEach
  override fun setUp() {
    super.setUp()
    this.purgeIntegrationQueues()
    mnAuth.stubGrantToken()
  }

  @AfterEach
  internal fun tearDown() {
    assertMessageQueuesAreEmpty()
  }

  @Nested
  @DisplayName("`PUT` $RESEND_JOBS_ENDPOINT")
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class ResendJobsEndpoint {
    private val registeredJobs = listOf(tescoWarehouseHandler, amazonForkliftOperator)
      .map { it.makeCopy() }
    private val notYetRegisteredJobs = listOf(abcConstructionApprentice)
      .map { it.makeCopy() }
    private val allJobs = registeredJobs + notYetRegisteredJobs
    private val endpoint = RESEND_JOBS_ENDPOINT

    @BeforeEach
    internal fun setUp() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllJobs(allJobs)
      jobsBoardApi.stubRetrieveJob(allJobs)

      givenEmployerExternalIds(allJobs.map { it.employerId })
      givenJobExternalIds(registeredJobs.map { it.id }).also {
        mnJobBoardApi.resetStates(nextJobId = it + 1)
      }

      mnJobBoardApi.stubCreateJob(notYetRegisteredJobs)
      mnJobBoardApi.stubUpdateJob()
    }

    @Test
    @Order(1)
    fun `discover and resend missing jobs`() {
      assertResendDataIsOk(endpoint)
    }

    @Test
    @Order(2)
    fun `discover and resend missing jobs, and only registering missing jobs`() {
      val expectedItemCount = notYetRegisteredJobs.size.toLong()
      val expectedTotalCount = allJobs.size.toLong()
      assertResendDataIsExpected(endpoint, expectedItemCount, expectedTotalCount)
    }

    @Nested
    @DisplayName("Some job IDs have been given.")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class GivenJobIds {
      private val notYetRegisterJob = abcConstructionApprentice
      private val registeredJob = tescoWarehouseHandler
      private val jobIds = listOf(notYetRegisterJob, registeredJob).map { it.id }
      private val jobCount = jobIds.size.toLong()

      @Order(1)
      @Test
      fun `resend given jobs, if not yet registered`() {
        val requestBody = makeRequestBody(jobIds)
        assertResendDataIsExpected(endpoint, 1, jobCount, requestBody)
      }

      @Order(2)
      @Test
      fun `resend given jobs, without force-update`() {
        val requestBody = makeRequestBody(jobIds, forceUpdate = false)
        assertResendDataIsExpected(endpoint, 1, jobCount, requestBody)
      }

      @Order(3)
      @Test
      fun `resend given jobs, with force-update`() {
        val requestBody = makeRequestBody(jobIds, forceUpdate = true)

        assertResendDataIsExpected(endpoint, jobCount, jobCount, requestBody)
      }
    }
  }

  private fun makeRequestBody(jobIds: List<String>) = makeRequestBody(jobIds, IDS_FIELD_NAME)
  private fun makeRequestBody(jobIds: List<String>, forceUpdate: Boolean = false) = makeRequestBody(jobIds, forceUpdate, IDS_FIELD_NAME)
}
