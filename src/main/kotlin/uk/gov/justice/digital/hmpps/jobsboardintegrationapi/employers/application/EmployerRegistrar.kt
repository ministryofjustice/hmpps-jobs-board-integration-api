package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

@Service
class EmployerRegistrar {
  fun registerCreation(employer: Employer) {
    // TODO implement employer registration to MN job-board API
    throw NotImplementedError("Employer-Creation's registration is not yet implemented!")
  }
}
