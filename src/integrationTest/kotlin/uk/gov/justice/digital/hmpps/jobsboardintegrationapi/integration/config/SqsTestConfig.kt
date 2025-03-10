package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.INTEGRATION_QUEUE_ID
import uk.gov.justice.hmpps.sqs.HmppsQueueFactory
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException

@TestConfiguration
class SqsTestConfig(private val hmppsQueueFactory: HmppsQueueFactory) {

  @Bean("integrationqueue-sqs-client")
  fun integrationqueueSqsClient(
    hmppsSqsProperties: HmppsSqsProperties,
  ): SqsAsyncClient = with(hmppsSqsProperties) {
    val config = queues[INTEGRATION_QUEUE_ID]
      ?: throw MissingQueueException("HmppsSqsProperties config for $INTEGRATION_QUEUE_ID not found")
    hmppsQueueFactory.createSqsAsyncClient(config, hmppsSqsProperties, null)
  }
}
