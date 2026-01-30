package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Spy
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.IntegrationMessageService
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType.EXPRESSION_OF_INTEREST_CREATED
import kotlin.test.assertFailsWith

class HmppsMessageServiceShould : HmppsMessageServiceTestCase() {
  @Spy
  @InjectMocks
  private lateinit var dummyHmppsMessageService: DummyHmppsMessageService

  @InjectMocks
  private lateinit var emptyHmppsMessageService: EmptyHmppsMessageService

  @Nested
  @DisplayName("Given a HMPPS message")
  inner class GivenHmppsMessage {
    private val message = makeHmppsMessage()

    @Test
    fun `convert message to HMPPS Message, and handle it`() {
      givenHmppsMessage()

      dummyHmppsMessageService.handleMessage(
        integrationEvent = message.integrationEvent(),
        messageAttributes = message.integrationMessageAttributes(),
      )

      val messageCaptor = argumentCaptor<HmppsMessage>()
      val eventTypeCaptor = argumentCaptor<HmppsMessageEventType>()
      verify(dummyHmppsMessageService).dummyHandle(messageCaptor.capture(), eventTypeCaptor.capture())

      assertThat(messageCaptor.firstValue).isEqualTo(message)
      assertThat(eventTypeCaptor.firstValue).isEqualTo(EXPRESSION_OF_INTEREST_CREATED)
    }
  }

  @Nested
  @DisplayName("Given an invalid HMPPS message")
  inner class GivenInvalidHmppsMessage {
    @Test
    fun `throw exception, when event type is missing`() {
      val message = makeHmppsMessage(eventTypeCode = null)

      assertFailsWithErrorMessage(message, "Missing event type!")
    }

    @Test
    fun `throw exception, when event type is unknown`() {
      val eventTypeCode = "UNKNOWN_EVENT"
      val message = makeHmppsMessage(eventTypeCode)

      assertFailsWithErrorMessage(message, "Unknown event type: $eventTypeCode")
    }

    @Test
    fun `throw exception, when event type is unexpected`() {
      val eventTypeCode = EXPRESSION_OF_INTEREST_CREATED.type
      val message = makeHmppsMessage(eventTypeCode)

      assertFailsWithErrorMessage(emptyHmppsMessageService, message, "Unexpected event type: $eventTypeCode")
    }

    private fun assertFailsWithErrorMessage(
      hmppsMessage: HmppsMessage,
      vararg errorMessages: String,
    ) = assertFailsWithErrorMessage(dummyHmppsMessageService, hmppsMessage, *errorMessages)

    private fun assertFailsWithErrorMessage(
      messageService: IntegrationMessageService,
      hmppsMessage: HmppsMessage,
      vararg errorMessages: String,
    ) {
      val exception = assertFailsWith<Exception> {
        with(hmppsMessage) {
          messageService.handleMessage(integrationEvent(), integrationMessageAttributes())
        }
      }

      assertThat(exception.message).contains(*errorMessages, "eventId=${hmppsMessage.messageId}")
    }
  }

  private fun givenHmppsMessage() {
    makeHmppsMessage()
    wheneverParseJsonOfHmppsMessage()
  }

  private fun wheneverParseJsonOfHmppsMessage() {
    whenever(mockedJsonMapper.readValue(anyString(), eq(HmppsMessage::class.java)))
      .thenAnswer { jsonMapper.readValue(it.arguments[0].toString(), HmppsMessage::class.java) }
  }

  private fun makeHmppsMessage() = EXPRESSION_OF_INTEREST_CREATED.let { eventType ->
    HmppsMessage(randomUUID(), eventType, messageAttributes = makeMessageAttributes(eventType))
  }

  private fun makeHmppsMessage(eventTypeCode: String? = null) = EXPRESSION_OF_INTEREST_CREATED.let { eventType ->
    HmppsMessage(
      messageId = randomUUID(),
      eventType = eventType,
      messageAttributes = eventTypeCode?.let { makeMessageAttributes(it) } ?: emptyMap(),
    )
  }
}

private class DummyHmppsMessageService(jsonMapper: JsonMapper) : HmppsMessageServiceBase(EXPRESSION_OF_INTEREST_CREATED, jsonMapper) {

  override fun handleHmppsMessage(hmppsMessage: HmppsMessage, eventType: HmppsMessageEventType) {
    dummyHandle(hmppsMessage, eventType)
  }

  fun dummyHandle(hmppsMessage: HmppsMessage, eventType: HmppsMessageEventType) {}
}

private class EmptyHmppsMessageService(jsonMapper: JsonMapper) : HmppsMessageServiceBase(emptySet(), jsonMapper) {
  override fun handleHmppsMessage(hmppsMessage: HmppsMessage, eventType: HmppsMessageEventType) {
    fail<Unit>("This should be unreachable!")
  }
}
