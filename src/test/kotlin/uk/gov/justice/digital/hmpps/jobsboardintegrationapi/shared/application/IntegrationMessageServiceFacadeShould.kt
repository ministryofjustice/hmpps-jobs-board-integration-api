package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Qualifier
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.EventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MessageAttributes
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class IntegrationMessageServiceFacadeShould : UnitTestBase() {
  @Mock
  @Qualifier("integrationServiceMap")
  private lateinit var serviceMap: Map<String, IntegrationMessageService>

  @InjectMocks
  private lateinit var serviceFacade: IntegrationMessageServiceFacade

  @Test
  fun `throw exception, when missing event type`() {
    val integrationEvent = dummyIntegrationEvent()
    val messageAttributes = MessageAttributes()
    val exception = assertFailsWith<IllegalArgumentException> {
      serviceFacade.handleMessage(integrationEvent, messageAttributes)
    }

    val expectedError = "Missing event type eventId=${integrationEvent.eventId}"
    assertEquals(expectedError, exception.message)
  }

  @Nested
  @DisplayName("Given no integration service has been configured")
  inner class GivenNoService {

    @BeforeEach
    internal fun setUp() {
      whenever(serviceMap[any()]).thenReturn(null)
    }

    @Test
    fun `throw exception, when message received but no service to handle`() {
      val integrationEvent = dummyIntegrationEvent()
      val messageAttributes = MessageAttributes(dummyEventType())
      val exception = assertFailsWith<IllegalArgumentException> {
        serviceFacade.handleMessage(integrationEvent, messageAttributes)
      }

      val expectedError =
        "MessageService not found for Event type=${messageAttributes.eventType}, eventId=${integrationEvent.eventId}"
      assertEquals(expectedError, exception.message)
    }
  }

  @Nested
  @DisplayName("Given an integration service has been configured")
  inner class GivenAnIntegrationService {
    @Mock
    private lateinit var dummyIntegrationMessageService: IntegrationMessageService

    @BeforeEach
    internal fun setUp() {
      whenever(serviceMap[any()]).thenReturn(dummyIntegrationMessageService)
    }

    @Test
    fun `send message to designated service`() {
      val integrationEvent = dummyIntegrationEvent()
      val messageAttributes = MessageAttributes(dummyEventType())
      serviceFacade.handleMessage(integrationEvent, messageAttributes)

      val eventCaptor = argumentCaptor<IntegrationEvent>()
      val attributesCaptor = argumentCaptor<MessageAttributes>()
      verify(dummyIntegrationMessageService).handleMessage(eventCaptor.capture(), attributesCaptor.capture())

      val actualEvent = eventCaptor.firstValue
      val actualMessageAttributes = attributesCaptor.firstValue
      assertThat(actualEvent).isEqualTo(integrationEvent)
      assertThat(actualMessageAttributes).isEqualTo(messageAttributes)
    }
  }

  private fun integrationEvent(eventType: String, content: String = "") = IntegrationEvent(
    eventId = randomUUID(),
    eventType = eventType,
    timestamp = defaultCurrentTime,
    content = content,
  )

  private fun dummyEventType() = EventType("mjma-jobs-board-dummy-created")

  private fun dummyIntegrationEvent() = integrationEvent("DummyCreated")
}
