package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerMother.sainsburys
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.wiremock.JobsBoardApiExtension.Companion.jobsBoardApi
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.JobsBoardApiWebClient

class JobsBoardApiWebClientShould : IntegrationTestBase() {

  @Autowired
  private lateinit var jobsBoardApiWebClient: JobsBoardApiWebClient

  @Nested
  @DisplayName("JobsBoard `GET` /employers/{id}")
  inner class EmployersGetDetailsEndpoint {
    @Test
    fun `return employer details, given valid employer ID`() {
      val employer = sainsburys.copy(createdAt = timeProvider.nowAsInstant())

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveEmployer(employer)

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employer.id)

      assertThat(actualEmployer).isEqualTo(employer)
    }

    @Test
    fun `return nothing, given invalid employer ID`() {
      val employerId = randomUUID()

      hmppsAuth.stubGrantToken()
      jobsBoardApi.stubRetrieveEmployerNotFound()

      val actualEmployer = jobsBoardApiWebClient.getEmployer(employerId)

      assertThat(actualEmployer).isNull()
    }
  }
}
