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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerObjects.sainsburys
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class EmployerUpdateMessageServiceShould : EmployerMessageServiceTestCase() {

  @InjectMocks
  private lateinit var updateService: EmployerUpdateMessageService

  @BeforeEach
  internal fun setUp() {
    whenever(mockedObjectMapper.readValue(anyString(), eq(EmployerEvent::class.java))).thenAnswer {
      objectMapper.readValue(it.arguments[0] as String, EmployerEvent::class.java)
    }
  }

  @Nested
  @DisplayName("Given an existing employer")
  inner class GivenExistingEmployer {
    private val employer = sainsburys

    @BeforeEach
    internal fun setUp() {
      whenever(employerRetriever.retrieve(employer.id)).thenReturn(employer)
    }

    @Test
    fun `receive and handle the event of Employer Update`() {
      val employerId = sainsburys.id
      val employerEvent = employerUpdateEvent(employerId)

      updateService.handleMessage(employerEvent.toIntegrationEvent(), employerEvent.messageAttributes())

      argumentCaptor<String>().let { captor ->
        verify(employerRetriever).retrieve(captor.capture())
        assertEquals(employerId, captor.firstValue)
      }

      argumentCaptor<Employer>().let { captor ->
        verify(employerRegistrar).registerUpdate(captor.capture())
        assertEquals(employer, captor.firstValue)
      }
    }

    @Test
    fun `throw exception, when receive message, but fail to retrieve employer details`() {
      val causeOfException = RuntimeException("Error retrieving employer details")

      throwsExceptionWhenRetrievingEmployer(causeOfException, employer.id)
    }

    @Test
    fun `throw exception, when receive message, but fail to update employer (ID mapping missing)`() {
      val causeOfException = IllegalStateException("Employer with id=${employer.id} not found (ID mapping missing)")

      throwsExceptionWhenRegisteringUpdate(causeOfException, employer)
    }

    @Test
    fun `throw exception, when receive message, but fail to update employer (error updating in downstream)`() {
      val causeOfException = RuntimeException("Error updating employer at MN")

      throwsExceptionWhenRegisteringUpdate(causeOfException, employer)
    }
  }

  @Nested
  @DisplayName("Given non-existent employer")
  inner class GivenNonExistentEmployer {
    @Test
    fun `throw exception, when receive message, with invalid employer ID`() {
      val employerId = randomUUID()
      val causeOfException = IllegalArgumentException("Employer id=$employerId not found")
      whenever(employerRetriever.retrieve(employerId)).thenThrow(causeOfException)

      throwsExceptionWhenUpdatingEmployer(causeOfException, employerId)
    }
  }

  private fun throwsExceptionWhenRetrievingEmployer(causeOfException: Throwable, employerId: String) {
    whenever(employerRetriever.retrieve(employerId)).thenThrow(causeOfException)

    throwsExceptionWhenUpdatingEmployer(causeOfException, employerId)
  }

  private fun throwsExceptionWhenRegisteringUpdate(causeOfException: Throwable, employer: Employer) {
    whenever(employerRegistrar.registerUpdate(employer)).thenThrow(causeOfException)

    throwsExceptionWhenUpdatingEmployer(causeOfException, employer.id)
  }

  private fun throwsExceptionWhenUpdatingEmployer(causeOfException: Throwable, employerId: String) {
    val employerEvent = employerUpdateEvent(employerId)
    val expectedException =
      Exception("Error at employer update event: eventId=${employerEvent.eventId}", causeOfException)

    val actualException = assertFailsWith<Exception> {
      updateService.handleMessage(employerEvent.toIntegrationEvent(), employerEvent.messageAttributes())
    }

    assertEquals(expectedException.message, actualException.message)
  }

  private fun employerUpdateEvent(employerId: String) = EmployerEvent(
    eventId = randomUUID(),
    eventType = EmployerEventType.EMPLOYER_UPDATED,
    timestamp = defaultCurrentTime,
    employerId = employerId,
  )
}
