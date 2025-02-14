package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.ExpressionOfInterest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient

@ConditionalOnIntegrationEnabled
@Service
class ExpressionOfInterestRegistrar(
  private val jobsBoardApiClient: JobsBoardApiClient,
  private val jobExternalIdRepository: JobExternalIdRepository,
) {
  fun registerCreation(jobExternalId: Long, prisonNumber: String) {
    val jobId = retrieveIdByExternalId(jobExternalId)
    try {
      requireNotNull(jobId) { "Job ID not found! jobExtId=$jobExternalId" }
      jobsBoardApiClient.createExpressionOfInterest(ExpressionOfInterest(jobId, prisonNumber))
    } catch (throwable: Throwable) {
      "Fail to register expression-of-interest; jobExtId=$jobExternalId, prisonNumber=$prisonNumber".let { message ->
        throw Exception(message, throwable)
      }
    }
  }

  private fun retrieveIdByExternalId(extId: Long): String? = jobExternalIdRepository.findByKeyExternalId(extId)?.key?.id
}
