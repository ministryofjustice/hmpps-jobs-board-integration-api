package uk.gov.justice.digital.hmpps.jobsboardintegrationapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsJobsBoardIntegrationApi

fun main(args: Array<String>) {
  runApplication<HmppsJobsBoardIntegrationApi>(*args)
}
