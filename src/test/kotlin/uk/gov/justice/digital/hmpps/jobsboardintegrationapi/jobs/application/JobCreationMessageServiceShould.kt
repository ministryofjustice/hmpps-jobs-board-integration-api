package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEvent
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobEventType
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.abcConstructionApprentice
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class JobCreationMessageServiceShould : JobMessageServiceTestCase() {

  @InjectMocks
  private lateinit var creationService: JobCreationMessageService

  @BeforeEach
  internal fun setUp() {
    whenever(mockedObjectMapper.readValue(anyString(), eq(JobEvent::class.java))).thenAnswer {
      objectMapper.readValue(it.arguments[0] as String, JobEvent::class.java)
    }
  }

  @Nested
  @DisplayName("Given a new job")
  inner class GivenANewJob {
    private val job = abcConstructionApprentice

    @BeforeEach
    internal fun setUp() {
      whenever(jobRetriever.retrieve(job.id)).thenReturn(job)
    }

    @Test
    fun `receive and handle the event of Job Creation`() {
      val jobId = job.id
      val jobEvent = jobCreationEvent(jobId)

      creationService.handleMessage(jobEvent.toIntegrationEvent(), jobEvent.messageAttributes())

      argumentCaptor<String>().let { captor ->
        verify(jobRetriever).retrieve(captor.capture())
        assertEquals(jobId, captor.firstValue)
      }

      argumentCaptor<Job>().let { captor ->
        verify(jobRegistrar).registerCreation(captor.capture())
        assertEquals(job, captor.firstValue)
      }
    }
  }

  @Test
  fun `receive and hit error handling event, with invalid job ID`() {
    val jobId = randomUUID()
    val jobEvent = jobCreationEvent(jobId)
    val errorCause = IllegalArgumentException("Job id=$jobId not found")
    whenever(jobRetriever.retrieve(anyString())).thenThrow(errorCause)

    val exception = assertFailsWith<Exception> {
      creationService.handleMessage(jobEvent.toIntegrationEvent(), jobEvent.messageAttributes())
    }
    assertEquals("Error at job creation event: eventId=${jobEvent.eventId}", exception.message)
    assertEquals(errorCause, exception.cause)
  }

  private fun jobCreationEvent(jobId: String) = JobEvent(
    eventId = randomUUID(),
    eventType = JobEventType.JOB_CREATED,
    timestamp = defaultCurrentTime,
    jobId = jobId,
  )
}
