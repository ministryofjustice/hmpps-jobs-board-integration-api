package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.HmppsMessageServiceTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessage
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.HmppsMessageEventType.EXPRESSION_OF_INTEREST_CREATED
import kotlin.test.assertFailsWith

class ExpressionOfInterestMessageServiceShould : HmppsMessageServiceTestCase() {
  @InjectMocks
  private lateinit var messageService: ExpressionOfInterestMessageService

  @Mock
  private lateinit var eoiRegistrar: ExpressionOfInterestRegistrar

  private val prisonNumber = "A1234BC"

  @BeforeEach
  internal fun setUp() {
    whenever(mockedJsonMapper.readValue(anyString(), eq(HmppsMessage::class.java))).thenAnswer {
      jsonMapper.readValue(it.arguments[0] as String, HmppsMessage::class.java)
    }
  }

  @Nested
  @DisplayName("Given an existing job")
  inner class GivenExistingJob {
    private val jobExternalId = 101L

    @Test
    fun `receive and handle the event of Expression of Interest creation`() {
      expressionOfInterestMessage(jobExternalId).run {
        messageService.handleMessage(integrationEvent(), integrationMessageAttributes())
      }

      val jobExtIdCaptor = argumentCaptor<Long>()
      val prisonNumberCaptor = argumentCaptor<String>()
      verify(eoiRegistrar).registerCreation(jobExtIdCaptor.capture(), prisonNumberCaptor.capture())
      assertEquals(jobExternalId, jobExtIdCaptor.firstValue)
      assertEquals(prisonNumber, prisonNumberCaptor.firstValue)
    }

    @Test
    fun `throw exception, when creating expression of interest via API client`() {
      val errorMessage = "Some server error occurred creating Expression-of-Interest"
      whenever(eoiRegistrar.registerCreation(jobExternalId, prisonNumber))
        .thenThrow(RuntimeException(errorMessage))

      assertFailsWithErrorMessage(expressionOfInterestMessage(jobExternalId), errorMessage)
    }

    @Nested
    @DisplayName("And the message is invalid")
    inner class AndInvalidMessage {

      @Test
      fun `throw exception, when jobId is missing`() {
        val eoiMessage = incompleteExpressionOfInterestMessage(prisonNumber = prisonNumber)

        assertFailsWithErrorMessage(eoiMessage, "Missing jobId")
      }

      @Test
      fun `throw exception, when prisonNumber is missing`() {
        val eoiMessage = incompleteExpressionOfInterestMessage(jobId = jobExternalId)

        assertFailsWithErrorMessage(eoiMessage, "Missing prisonNumber")
      }

      @Test
      fun `throw exception, when jobId is invalid`() {
        val invalidJobId = "NaN"
        val eoiMessage = expressionOfInterestMessage(invalidJobId, prisonNumber)

        assertFailsWithErrorMessage(eoiMessage, "Invalid jobId=$invalidJobId")
      }

      @Test
      fun `throw exception, when prisonNumber is invalid`() {
        val cause = IllegalArgumentException("NotFound: Invalid Prison Number")
        val error = "Fail to register expression-of-interest; jobExtId=$jobExternalId, prisonNumber=$prisonNumber"
          .let { message -> RuntimeException(message, cause) }
        whenever(eoiRegistrar.registerCreation(jobExternalId, prisonNumber))
          .thenThrow(error)

        val eoiMessage = expressionOfInterestMessage(jobExternalId, prisonNumber)

        assertFailsWithErrorMessage(eoiMessage, error.message!!)
      }
    }
  }

  @Nested
  @DisplayName("Given non-existent job")
  inner class GivenNonExistentJob {
    @Test
    fun `throw exception, when Job ID mapping is missing`() {
      val jobExternalId = Long.MAX_VALUE
      val errorMessage = "Job ID not found! jobExtId=$jobExternalId"
      whenever(eoiRegistrar.registerCreation(jobExternalId, prisonNumber))
        .thenThrow(RuntimeException(errorMessage))

      assertFailsWithErrorMessage(expressionOfInterestMessage(jobExternalId), errorMessage)
    }
  }

  private fun assertFailsWithErrorMessage(
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

  private fun expressionOfInterestMessage(jobId: Long = 1, prisonNumber: String = this.prisonNumber) = expressionOfInterestMessage(
    mapOf(
      "eventType" to EXPRESSION_OF_INTEREST_CREATED.type,
      "jobId" to jobId.toString(),
      "prisonNumber" to prisonNumber,
    ),
  )

  private fun incompleteExpressionOfInterestMessage(jobId: Long? = null, prisonNumber: String? = null) = expressionOfInterestMessage(
    mapOf(
      "eventType" to EXPRESSION_OF_INTEREST_CREATED.type,
      "jobId" to jobId?.toString(),
      "prisonNumber" to prisonNumber,
    ).filterValues { it != null }.mapValues { it.value!! },
  )

  private fun expressionOfInterestMessage(
    jobId: String,
    prisonNumber: String,
    eventType: String = EXPRESSION_OF_INTEREST_CREATED.type,
  ) = expressionOfInterestMessage(
    mapOf(
      "eventType" to eventType,
      "jobId" to jobId,
      "prisonNumber" to prisonNumber,
    ),
  )

  private fun expressionOfInterestMessage(messageAttributes: Map<String, String>) = HmppsMessage(
    messageId = randomUUID(),
    eventType = EXPRESSION_OF_INTEREST_CREATED,
    messageAttributes = messageAttributes,
  )
}
