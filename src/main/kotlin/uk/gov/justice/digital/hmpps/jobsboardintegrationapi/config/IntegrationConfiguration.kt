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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.ExpressionOfInterestMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.ExpressionOfInterestRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.JobCreationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.JobRegistrar
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.JobRetriever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application.JobUpdateMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.IntegrationMessageListener

@Configuration
@ConditionalOnIntegrationEnabled
class IntegrationConfiguration {
  @Bean
  @Qualifier("integrationServiceMap")
  fun integrationServiceMap(
    employerCreationMessageService: EmployerCreationMessageService,
    employerUpdateMessageService: EmployerUpdateMessageService,
    jobCreationMessageService: JobCreationMessageService,
    jobUpdateMessageService: JobUpdateMessageService,
    expressionOfInterestMessageService: ExpressionOfInterestMessageService,
  ): Map<String, IntegrationMessageService> = mapOf(
    EmployerEventType.EMPLOYER_CREATED.type to employerCreationMessageService,
    EmployerEventType.EMPLOYER_UPDATED.type to employerUpdateMessageService,
    JobEventType.JOB_CREATED.type to jobCreationMessageService,
    JobEventType.JOB_UPDATED.type to jobUpdateMessageService,
    HmppsMessageEventType.EXPRESSION_OF_INTEREST_CREATED.type to expressionOfInterestMessageService,
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
  fun jobCreationMessageService(
    retriever: JobRetriever,
    registrar: JobRegistrar,
    objectMapper: ObjectMapper,
  ) = JobCreationMessageService(retriever, registrar, objectMapper)

  @Bean
  fun jobUpdateMessageService(
    retriever: JobRetriever,
    registrar: JobRegistrar,
    objectMapper: ObjectMapper,
  ) = JobUpdateMessageService(retriever, registrar, objectMapper)

  @Bean
  fun expressionOfInterestMessageService(
    expressionOfInterestRegistrar: ExpressionOfInterestRegistrar,
    objectMapper: ObjectMapper,
  ) = ExpressionOfInterestMessageService(expressionOfInterestRegistrar, objectMapper)

  @Bean
  fun integrationMessageListener(
    @Qualifier("integrationMessageService") integrationMessageService: IntegrationMessageService,
  ) = IntegrationMessageListener(integrationMessageService)
}
