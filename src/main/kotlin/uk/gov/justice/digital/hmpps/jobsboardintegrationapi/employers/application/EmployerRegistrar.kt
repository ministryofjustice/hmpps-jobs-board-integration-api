package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

@ConditionalOnIntegrationEnabled
@Service
class EmployerRegistrar(
  private val employerService: EmployerService,
) {
  fun registerCreation(employer: Employer) {
    try {
      val mnEmployer = employerService.run { create(convert(employer)) }
      assert(mnEmployer.id != null) { "MN Employer ID is missing! employerId=${employer.id}, employerName=${employer.name}" }
      employerService.createIdMapping(mnEmployer.id!!, employer.id)
    } catch (ex: Exception) {
      "Fail to register employer-creation; employerId=${employer.id}, employerName=${employer.name}".let { message ->
        throw Exception(message, ex)
      }
    }
  }
}
