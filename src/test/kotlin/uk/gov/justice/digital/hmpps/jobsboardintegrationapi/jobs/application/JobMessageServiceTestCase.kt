package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.mockito.Mock
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class JobMessageServiceTestCase : UnitTestBase() {
  @Mock
  protected lateinit var jobRetriever: JobRetriever

  @Mock
  protected lateinit var jobRegistrar: JobRegistrar

  protected fun JobEvent.messageAttributes() = MessageAttributes(this.eventType.type)

  protected fun JobEvent.toIntegrationEvent() = IntegrationEvent(
    eventId = this.eventId,
    eventType = this.eventType.type,
    message = objectMapper.writeValueAsString(this),
  )
}
