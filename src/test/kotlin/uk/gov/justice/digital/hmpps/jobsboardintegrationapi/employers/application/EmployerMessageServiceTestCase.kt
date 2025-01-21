package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.mockito.Mock
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.EventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes

abstract class EmployerMessageServiceTestCase : UnitTestBase() {
  @Mock
  protected lateinit var employerRetriever: EmployerRetriever

  @Mock
  protected lateinit var employerRegistrar: EmployerRegistrar

  protected fun EmployerEvent.messageAttributes() = MessageAttributes(this.eventTypeAsAttribute())

  protected fun EmployerEvent.eventTypeAsAttribute() = EventType(this.eventType.type)

  protected fun EmployerEvent.toIntegrationEvent() = IntegrationEvent(
    eventId = this.eventId,
    eventType = this.eventType.type,
    timestamp = this.timestamp,
    content = objectMapper.writeValueAsString(this),
  )
}
