package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ServiceTestCase
import kotlin.test.assertFailsWith

class JobRetrieverShould : ServiceTestCase() {
  @InjectMocks
  private lateinit var jobRetriever: JobRetriever

  @Test
  fun `return valid job`() {
    val job = tescoWarehouseHandler
    val jobId = job.id
    whenever(jobsBoardApiClient.getJob(jobId)).thenReturn(job)

    val actualJob = jobRetriever.retrieve(jobId)

    assertThat(actualJob).isEqualTo(job)
  }

  @Test
  fun `throw exception when job not found`() {
    val jobId = randomUUID()
    whenever(jobsBoardApiClient.getJob(jobId)).thenReturn(null)

    val exception = assertFailsWith<IllegalArgumentException> { jobRetriever.retrieve(jobId) }

    assertThat(exception.message).isEqualTo("Job id=$jobId not found")
  }
}
