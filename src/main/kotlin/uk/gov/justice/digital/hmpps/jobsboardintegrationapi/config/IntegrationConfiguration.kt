package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerCreationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerRetriever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.IntegrationMessageListener

@Configuration
@ConditionalOnProperty("api.integration.enabled", havingValue = "true")
class IntegrationConfiguration {
  @Bean
  @Qualifier("integrationServiceMap")
  fun integrationServiceMap(
    employerCreationMessageService: EmployerCreationMessageService,
  ): Map<String, IntegrationMessageService> = mapOf(
    EmployerEventType.EMPLOYER_CREATED.type to employerCreationMessageService,
  )

  @Bean
  fun employerCreationMessageService(
    retriever: EmployerRetriever,
    registrar: EmployerRegistrar,
    objectMapper: ObjectMapper,
  ) = EmployerCreationMessageService(retriever, registrar, objectMapper)

  @Bean
  fun integrationMessageListener(
    @Qualifier("integrationMessageService") integrationMessageService: IntegrationMessageService,
    objectMapper: ObjectMapper,
  ) = IntegrationMessageListener(integrationMessageService, objectMapper)
}
