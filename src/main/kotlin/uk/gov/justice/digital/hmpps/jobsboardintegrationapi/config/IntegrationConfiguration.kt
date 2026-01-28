package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.IntegrationEventService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventEmitter
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.EventQueueEmitter
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.IntegrationMessageListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService

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
    jsonMapper: JsonMapper,
  ) = EmployerCreationMessageService(retriever, registrar, jsonMapper)

  @Bean
  fun employerUpdateMessageService(
    retriever: EmployerRetriever,
    registrar: EmployerRegistrar,
    jsonMapper: JsonMapper,
  ) = EmployerUpdateMessageService(retriever, registrar, jsonMapper)

  @Bean
  fun jobCreationMessageService(
    retriever: JobRetriever,
    registrar: JobRegistrar,
    jsonMapper: JsonMapper,
  ) = JobCreationMessageService(retriever, registrar, jsonMapper)

  @Bean
  fun jobUpdateMessageService(
    retriever: JobRetriever,
    registrar: JobRegistrar,
    jsonMapper: JsonMapper,
  ) = JobUpdateMessageService(retriever, registrar, jsonMapper)

  @Bean
  fun expressionOfInterestMessageService(
    expressionOfInterestRegistrar: ExpressionOfInterestRegistrar,
    jsonMapper: JsonMapper,
  ) = ExpressionOfInterestMessageService(expressionOfInterestRegistrar, jsonMapper)

  @Bean
  fun integrationMessageListener(
    @Qualifier("integrationMessageService") integrationMessageService: IntegrationMessageService,
  ) = IntegrationMessageListener(integrationMessageService)

  @Bean
  fun integrationEventService(eventEmitter: EventEmitter) = IntegrationEventService(eventEmitter)

  @Bean
  fun eventEmitter(hmppsQueueService: HmppsQueueService): EventEmitter = EventQueueEmitter(hmppsQueueService).apply { forceInit() }
}
