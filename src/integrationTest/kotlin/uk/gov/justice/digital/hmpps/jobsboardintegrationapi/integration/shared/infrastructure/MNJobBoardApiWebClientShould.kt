package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.mnEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.MNJobBoardApiExtension.Companion.mnJobBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreatEmployerRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNEmployer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJobBoardApiWebClient
import kotlin.test.assertFailsWith

class MNJobBoardApiWebClientShould : IntegrationTestBase() {

  @Autowired
  private lateinit var apiWebClient: MNJobBoardApiWebClient

  @Nested
  @DisplayName("MN JobBoard `POST` /employers")
  inner class EmployersPostEndpoint {
    private val employer = sainsburys
    private val mnEmployer: MNEmployer get() = employer.copy(createdAt = timeProvider.nowAsInstant()).mnEmployer()

    @Test
    fun `create employer, with valid details`() {
      val expectedEmployer = mnEmployer.copy(id = 1L)
      mnJobBoardApi.stubCreateEmployer(mnEmployer, expectedEmployer.id!!)

      val actualEmployer = CreatEmployerRequest.from(mnEmployer).let { apiWebClient.createEmployer(it) }

      assertThat(actualEmployer).isEqualTo(expectedEmployer)
    }

    @Test
    fun `receive unauthorised error, if API access token is invalid`() {
      mnJobBoardApi.stubCreateEmployerUnauthorised()

      val exception = assertFailsWith<Exception> {
        CreatEmployerRequest.from(mnEmployer).let { apiWebClient.createEmployer(it) }
      }

      with(exception) {
        assertThat(message).contains("Fail to create employer") // reactive throw (ReactiveException)
        with(cause!!) {
          assertThat(message).contains("Fail to create employer") // actual throw (Exception)
          with(cause!!) {
            assertThat(message).contains("401 Unauthorized") // cause (401) (WebClientResponseException$Unauthorized)
          }
        }
      }
    }
  }
}
