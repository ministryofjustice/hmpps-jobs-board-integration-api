package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerMother.sainsburys
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class EmployerCreationMessageServiceShould : EmployerMessageServiceTestCase() {

  @InjectMocks
  private lateinit var creationService: EmployerCreationMessageService

  @BeforeEach
  internal fun setUp() {
    whenever(mockedObjectMapper.readValue(anyString(), eq(EmployerEvent::class.java))).thenAnswer {
      objectMapper.readValue(it.arguments[0] as String, EmployerEvent::class.java)
    }
  }

  @Nested
  @DisplayName("Given a new employer")
  inner class GivenANewEmployer {
    private val employer = sainsburys

    @BeforeEach
    internal fun setUp() {
      whenever(employerRetriever.retrieve(employer.id)).thenReturn(employer)
    }

    @Test
    fun `receive and handle the event of Employer Creation`() {
      val employerId = sainsburys.id
      val employerEvent = employerCreationEvent(employerId)

      creationService.handleMessage(employerEvent.toIntegrationEvent(), employerEvent.messageAttributes())

      argumentCaptor<String>().let { captor ->
        verify(employerRetriever).retrieve(captor.capture())
        assertEquals(employerId, captor.firstValue)
      }

      argumentCaptor<Employer>().let { captor ->
        verify(employerRegistrar).registerCreation(captor.capture())
        assertEquals(employer, captor.firstValue)
      }
    }
  }

  @Test
  fun `receive and hit error handling event, with invalid employer ID`() {
    val employerId = randomUUID()
    val employerEvent = employerCreationEvent(employerId)
    val errorCause = IllegalArgumentException("Employer id=$employerId not found")
    whenever(employerRetriever.retrieve(anyString())).thenThrow(errorCause)

    val exception = assertFailsWith<Exception> {
      creationService.handleMessage(employerEvent.toIntegrationEvent(), employerEvent.messageAttributes())
    }
    assertEquals("Error at employer creation event: eventId=${employerEvent.eventId}", exception.message)
    assertEquals(errorCause, exception.cause)
  }

  private fun employerCreationEvent(employerId: String) = EmployerEvent(
    eventId = randomUUID(),
    eventType = EmployerEventType.EMPLOYER_CREATED,
    timestamp = defaultCurrentTime,
    employerId = employerId,
  )
}
