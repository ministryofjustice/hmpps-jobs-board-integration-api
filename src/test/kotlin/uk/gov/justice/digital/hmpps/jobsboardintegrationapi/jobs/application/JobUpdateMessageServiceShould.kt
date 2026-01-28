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
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.nationalTescoWarehouseHandler
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class JobUpdateMessageServiceShould : JobMessageServiceTestCase() {

  @InjectMocks
  private lateinit var updateService: JobUpdateMessageService

  @BeforeEach
  internal fun setUp() {
    whenever(mockedJsonMapper.readValue(anyString(), eq(JobEvent::class.java))).thenAnswer {
      jsonMapper.readValue(it.arguments[0] as String, JobEvent::class.java)
    }
  }

  @Nested
  @DisplayName("Given an existing job")
  inner class GivenExistingJob {
    private val job = amazonForkliftOperator

    @BeforeEach
    internal fun setUp() {
      whenever(jobRetriever.retrieve(job.id)).thenReturn(job)
    }

    @Test
    fun `receive and handle the event of Job Update`() {
      val jobId = job.id
      val jobEvent = jobUpdateEvent(jobId)

      updateService.handleMessage(jobEvent.toIntegrationEvent(), jobEvent.messageAttributes())

      argumentCaptor<String>().let { captor ->
        verify(jobRetriever).retrieve(captor.capture())
        assertEquals(jobId, captor.firstValue)
      }

      argumentCaptor<Job>().let { captor ->
        verify(jobRegistrar).registerUpdate(captor.capture())
        assertEquals(job, captor.firstValue)
      }
    }

    @Test
    fun `throw exception, when receive message, but fail to retrieve job details`() {
      val causeOfException = RuntimeException("Error retrieving job details")

      throwsExceptionWhenRetrievingJob(causeOfException, job.id)
    }

    @Test
    fun `throw exception, when receive message, but fail to update job (ID mapping missing)`() {
      val causeOfException = IllegalStateException("Job with id=${job.id} not found (ID mapping missing)")

      throwsExceptionWhenRegisteringUpdate(causeOfException, job)
    }

    @Test
    fun `throw exception, when receive message, but fail to update job (error updating in downstream)`() {
      val causeOfException = RuntimeException("Error updating job at MN")

      throwsExceptionWhenRegisteringUpdate(causeOfException, job)
    }
  }

  @Nested
  @DisplayName("Given an existing national job")
  inner class GivenExistingNationalJob {
    private val job = nationalTescoWarehouseHandler

    @BeforeEach
    internal fun setUp() {
      whenever(jobRetriever.retrieve(job.id)).thenReturn(job)
    }

    @Test
    fun `receive and handle the event of Job Update`() {
      val jobId = job.id
      val jobEvent = jobUpdateEvent(jobId)

      updateService.handleMessage(jobEvent.toIntegrationEvent(), jobEvent.messageAttributes())

      argumentCaptor<String>().let { captor ->
        verify(jobRetriever).retrieve(captor.capture())
        assertEquals(jobId, captor.firstValue)
      }

      argumentCaptor<Job>().let { captor ->
        verify(jobRegistrar).registerUpdate(captor.capture())
        assertEquals(job, captor.firstValue)
      }
    }
  }

  @Nested
  @DisplayName("Given non-existent job")
  inner class GivenNonExistentJob {
    @Test
    fun `throw exception, when receive message, with invalid job ID`() {
      val jobId = randomUUID()
      val causeOfException = IllegalArgumentException("Job id=$jobId not found")
      whenever(jobRetriever.retrieve(jobId)).thenThrow(causeOfException)

      throwsExceptionWhenUpdatingJob(causeOfException, jobId)
    }
  }

  private fun throwsExceptionWhenRetrievingJob(causeOfException: Throwable, jobId: String) {
    whenever(jobRetriever.retrieve(jobId)).thenThrow(causeOfException)

    throwsExceptionWhenUpdatingJob(causeOfException, jobId)
  }

  private fun throwsExceptionWhenRegisteringUpdate(causeOfException: Throwable, job: Job) {
    whenever(jobRegistrar.registerUpdate(job)).thenThrow(causeOfException)

    throwsExceptionWhenUpdatingJob(causeOfException, job.id)
  }

  private fun throwsExceptionWhenUpdatingJob(causeOfException: Throwable, jobId: String) {
    val jobEvent = jobUpdateEvent(jobId)
    val expectedException =
      Exception("Error at job update event: eventId=${jobEvent.eventId}", causeOfException)

    val actualException = assertFailsWith<Exception> {
      updateService.handleMessage(jobEvent.toIntegrationEvent(), jobEvent.messageAttributes())
    }

    assertEquals(expectedException.message, actualException.message)
  }

  private fun jobUpdateEvent(jobId: String) = JobEvent(
    eventId = randomUUID(),
    eventType = JobEventType.JOB_UPDATED,
    timestamp = defaultCurrentTime,
    jobId = jobId,
  )
}
