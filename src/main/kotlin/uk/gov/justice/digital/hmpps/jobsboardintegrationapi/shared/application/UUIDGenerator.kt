package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.springframework.stereotype.Service
import java.util.*

@Service
class UUIDGenerator {
  fun generate(): String = UUID.randomUUID().toString()
}
