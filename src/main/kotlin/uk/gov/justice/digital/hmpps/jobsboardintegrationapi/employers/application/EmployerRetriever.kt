package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

@ConditionalOnIntegrationEnabled
@Service
class EmployerRetriever(
  private val employerService: EmployerService,
) {
  fun retrieve(id: String): Employer {
    return employerService.retrieveById(id) ?: run {
      throw IllegalArgumentException("Employer id=$id not found")
    }
  }
}
