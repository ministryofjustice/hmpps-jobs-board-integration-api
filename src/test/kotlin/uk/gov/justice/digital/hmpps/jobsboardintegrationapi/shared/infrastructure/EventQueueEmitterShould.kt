package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.EventData
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventQueueEmitterShould : UnitTestBase() {
  @Mock
  private lateinit var hmppsQueueService: HmppsQueueService

  @Mock
  private lateinit var integrationQueue: HmppsQueue

  @Mock
  private lateinit var sqsClient: SqsAsyncClient

  @InjectMocks
  private lateinit var eventQueueEmitter: EventQueueEmitter

  private lateinit var event: EventData

  @BeforeEach
  fun setup() {
    event = makeEvent()
  }

  @Test
  fun `send event`() {
    givenIntegrationQueueIsReady()
    stubMessageSent()

    eventQueueEmitter.send(event)

    val message = argumentCaptor<SendMessageRequest>().also { verify(sqsClient).sendMessage(it.capture()) }.firstValue
    with(message.messageAttributes()) {
      assertEquals(event.eventType, this["eventType"]?.stringValue())
      assertEquals(event.eventId, this["eventId"]?.stringValue())
    }
    assertThat(message.messageBody()).isEqualTo(event.content)
  }

  @Test
  fun `throw exception, when queue not found`() {
    whenever(hmppsQueueService.findByQueueId(any())).thenReturn(null)

    val exception = assertFailsWith<MissingQueueException> {
      eventQueueEmitter.send(event)
    }

    assertThat(exception).hasMessageContaining(INTEGRATION_QUEUE_ID)
  }

  @Test
  fun `throw exception, when fail to send message`() {
    givenIntegrationQueueIsReady()
    val errorMessage = "SQS fail to send message".also { stubMessageSent(RuntimeException(it)) }

    val exception = assertFailsWith<Throwable> {
      eventQueueEmitter.send(event)
    }

    assertThat(exception)
      .hasCauseInstanceOf(RuntimeException::class.java)
      .hasRootCauseMessage(errorMessage)
  }

  private fun stubMessageSent(error: Throwable? = null) {
    whenever(sqsClient.sendMessage(any<SendMessageRequest>())).thenReturn(
      error?.let { CompletableFuture.failedFuture(it) }
        ?: CompletableFuture.completedFuture(SendMessageResponse.builder().build()),
    )
  }

  private fun givenIntegrationQueueIsReady() {
    whenever(hmppsQueueService.findByQueueId(any())).thenReturn(integrationQueue)
    whenever(integrationQueue.sqsClient).thenReturn(sqsClient)
  }

  private fun makeEvent() = EventData(
    eventId = randomUUID(),
    eventType = "mjma-jobs-board.dummy.created",
    timestamp = defaultCurrentTime,
    content = "dummy has been created",
  )
}
