package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerCreationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerRetriever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application.EmployerUpdateMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.IntegrationMessageListener

@Configuration
@ConditionalOnIntegrationEnabled
class IntegrationConfiguration {
  @Bean
  @Qualifier("integrationServiceMap")
  fun integrationServiceMap(
    employerCreationMessageService: EmployerCreationMessageService,
    employerUpdateMessageService: EmployerUpdateMessageService,
  ): Map<String, IntegrationMessageService> = mapOf(
    EmployerEventType.EMPLOYER_CREATED.type to employerCreationMessageService,
    EmployerEventType.EMPLOYER_UPDATED.type to employerUpdateMessageService,
  )

  @Bean
  fun employerCreationMessageService(
    retriever: EmployerRetriever,
    registrar: EmployerRegistrar,
    objectMapper: ObjectMapper,
  ) = EmployerCreationMessageService(retriever, registrar, objectMapper)

  @Bean
  fun employerUpdateMessageService(
    retriever: EmployerRetriever,
    registrar: EmployerRegistrar,
    objectMapper: ObjectMapper,
  ) = EmployerUpdateMessageService(retriever, registrar, objectMapper)

  @Bean
  fun integrationMessageListener(
    @Qualifier("integrationMessageService") integrationMessageService: IntegrationMessageService,
  ) = IntegrationMessageListener(integrationMessageService)
}
