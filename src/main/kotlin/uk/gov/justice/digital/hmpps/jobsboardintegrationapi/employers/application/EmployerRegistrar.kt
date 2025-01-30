package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config.ConditionalOnIntegrationEnabled
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

@ConditionalOnIntegrationEnabled
@Service
class EmployerRegistrar(
  private val employerService: EmployerService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun registerCreation(employer: Employer) {
    if (employerService.existsIdMappingById(employer.id)) {
      log.warn("Employer with id={} already exists (with ID mapping), thus skipping", employer.id)
    } else {
      try {
        val mnEmployer = employerService.run { create(convert(employer)) }
        assert(mnEmployer.id != null) { "MN Employer ID is missing! employerId=${employer.id}, employerName=${employer.name}" }
        employerService.createIdMapping(mnEmployer.id!!, employer.id)
      } catch (throwable: Throwable) {
        "Fail to register employer-creation; employerId=${employer.id}, employerName=${employer.name}".let { message ->
          throw Exception(message, throwable)
        }
      }
    }
  }
}
