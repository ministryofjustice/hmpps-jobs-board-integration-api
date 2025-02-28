package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

// HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
@Component("hmppsAuth")
class HmppsAuthHealthPing(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

@Component("jobsBoardApi")
class JobsBoardApiHealthPing(@Qualifier("jobsBoardHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
