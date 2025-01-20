package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer

@Service
class EmployerRetriever {
  fun retrieve(id: String): Employer {
    // TODO implement employer retrieval from MJMA jobs board API
    throw NotImplementedError("Employer's retrieval is not yet implemented!")
  }
}
