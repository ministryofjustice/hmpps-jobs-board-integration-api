package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.resource.integrationadmin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.abcConstruction
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.amazon
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tesco
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.tescoLogistics
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension.Companion.jobsBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MNJobBoardApiExtension.Companion.mnJobBoardApi
import kotlin.test.Test
import kotlin.test.assertEquals

class ResendEmployersPutShould : ResendDataTestCase() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val RESEND_EMPLOYERS_ENDPOINT = "/integration-admin/resend-employers"
    private const val IDS_FIELD_NAME = "employerIds"
  }

  @BeforeEach
  override fun setUp() {
    super.setUp()
    this.purgeIntegrationQueues()
  }

  @AfterEach
  internal fun tearDown() {
    assertMessageQueuesAreEmpty()
  }

  @Nested
  @DisplayName("`PUT` $RESEND_EMPLOYERS_ENDPOINT")
  inner class ResendEmployersEndpoint {
    private val registeredEmployers = listOf(tesco, amazon, sainsburys)
      .map { it.copy(createdAt = defaultCurrentTime) }
    private val notYetRegisteredEmployers = listOf(tescoLogistics, abcConstruction)
      .map { it.copy(createdAt = defaultCurrentTime) }
    private val allEmployers = registeredEmployers + notYetRegisteredEmployers
    private val endpoint = RESEND_EMPLOYERS_ENDPOINT

    @BeforeEach
    internal fun setUp() {
      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveAllEmployers(allEmployers)
      jobsBoardApi.stubRetrieveEmployer(allEmployers)

      givenEmployerExternalIds(registeredEmployers.map { it.id }).also {
        mnJobBoardApi.resetStates(it + 1)
      }
      mnJobBoardApi.stubCreateEmployer(notYetRegisteredEmployers)
      registeredEmployers.forEach {
        employerExternalIdRepository.findByKeyId(it.id)!!.key.externalId.let { extId ->
          it.mnEmployer(extId)
            .also { log.debug("stub update: mnEmployer={}", it) }
            .let { mnJobBoardApi.stubUpdateEmployer(it) }
        }
      }
    }

    @Test
    fun `discover and resend missing employers`() {
      assertResendDataIsOk(endpoint)
    }

    @Test
    fun `discover and resend missing employers, and only registering missing employers`() {
      val expectedItemCount = notYetRegisteredEmployers.size.toLong()
      val expectedTotalCount = allEmployers.size.toLong()
      assertResendDataIsExpected(endpoint, expectedItemCount, expectedTotalCount)
    }

    @Nested
    @DisplayName("Some employer IDs have been given.")
    inner class GivenEmployerIds {
      private val notYetRegisterEmployer = tescoLogistics
      private val registeredEmployer = tesco
      private val employerIds = listOf(notYetRegisterEmployer, registeredEmployer).map { it.id }
      private val employerCount = employerIds.size.toLong()

      @Test
      fun `resend given employers, if not yet registered`() {
        val requestBody = makeRequestBody(employerIds)
        assertResendDataIsExpected(endpoint, 1, employerCount, requestBody)
      }

      @Test
      fun `resend given employers, without force-update`() {
        val requestBody = makeRequestBody(employerIds, forceUpdate = false)
        assertResendDataIsExpected(endpoint, 1, employerCount, requestBody)
      }

      @Test
      fun `resend given employers, with force-update`() {
        val requestBody = makeRequestBody(employerIds, forceUpdate = true)
        assertResendDataIsExpected(endpoint, employerCount, employerCount, requestBody)
      }
    }
  }

  private fun assertMessageQueuesAreEmpty() {
    log.debug("assertMessageQueuesAreEmpty|start")
    awaitIntegrationQueueAllMessagesAreGone(5)
    assertEquals(0, integrationDlqMessageCount())
    log.debug("assertMessageQueuesAreEmpty|end")
  }

  private fun makeRequestBody(employerIds: List<String>) = makeRequestBody(employerIds, IDS_FIELD_NAME)
  private fun makeRequestBody(employerIds: List<String>, forceUpdate: Boolean = false) = makeRequestBody(employerIds, forceUpdate, IDS_FIELD_NAME)
}
