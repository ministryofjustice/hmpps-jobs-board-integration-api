package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.abcConstructionApprentice
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.amazonForkliftOperator
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobObjects.tescoWarehouseHandler
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.mnJob
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.BASE_LOCATION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.CONTRACT_TYPE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.EMPLOYER_SECTOR
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.HOURS_PER_WEEK
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.JOB_SOURCE
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.OFFENCE_EXCLUSION
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.SALARY_PERIOD
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefData.WORK_PATTERN
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.ServiceTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.CreateJobRequest
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.MNJob
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.UpdateJobRequest
import kotlin.test.assertFailsWith

class JobRegistrarShould : ServiceTestCase() {
  @InjectMocks
  private lateinit var jobRegistrar: JobRegistrar

  @Nested
  @DisplayName("Given a new job to be created at MN")
  inner class GivenAJobToCreate {
    private val job = amazonForkliftOperator
    private val jobExtId = 1L
    private val employerExtId = 1001L
    private val mnJob = job.mnJob(employerExtId, jobExtId)
    private val mnJobNoId = mnJob.copy(id = null)

    @Test
    fun `register new job`() {
      givenAJobToCreate(job, mnJobNoId, mnJob)

      jobRegistrar.registerCreation(job)

      verify(jobExternalIdRepository).save(JobExternalId(job.id, jobExtId))
    }

    @Test
    fun `skip registering existing employer`() {
      givenJobExternalIDExists(job.id, jobExtId)

      jobRegistrar.registerCreation(job)

      verify(refDataMappingRepository, never()).findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(any(), any())
      verify(mnJobBoardApiClient, never()).createJob(any())
      verify(jobExternalIdRepository, never()).save(any())
    }

    @Nested
    @DisplayName("And with unexpected data or state")
    inner class AndUnexpectedDataOrState {
      @Test
      fun `NOT finish registering job without external ID received`() {
        val job = abcConstructionApprentice
        val mnJobNoId = job.mnJob(1002L)
        givenAJobToCreate(job, mnJobNoId)

        val exception = assertFailsWith<Exception> {
          jobRegistrar.registerCreation(job)
        }

        assertThat(exception.message)
          .startsWith("Fail to register job-creation").contains("jobId=${job.id}")
        assertThat(exception.cause)
          .isNotNull.isInstanceOfAny(AssertionError::class.java)
          .message().startsWith("MN Job ID is missing!").contains("jobId=${job.id}")
        verify(jobExternalIdRepository, never()).save(any())
      }

      @Test
      fun `NOT finish registering job, with ID mapping clash while saving it`() {
        val job = tescoWarehouseHandler
        val mnJobNoId = job.mnJob(1003L)
        val mnJob = mnJobNoId.copy(id = 3L)
        givenAJobToCreate(job, mnJobNoId, mnJob)
        job.id.let { id ->
          whenever(jobExternalIdRepository.findByKeyId(id)).thenReturn(null, JobExternalId(id, jobExtId))
        }

        val exception = assertFailsWith<Exception> { jobRegistrar.registerCreation(job) }

        assertThat(exception.message)
          .startsWith("Fail to register job-creation").contains("jobId=${job.id}")
        assertThat(exception.cause)
          .isNotNull.isInstanceOfAny(AssertionError::class.java)
          .message().startsWith("Job ID cannot be created! ID mapping already exists.")
      }
    }
  }

  @Nested
  @DisplayName("Given an invalid job")
  inner class GivenInvalidJobToCreate {
    @Test
    fun `NOT register a job with invalid employer ID`() {
      val invalidEmployerId = randomUUID()
      val job = abcConstructionApprentice.copy(employerId = invalidEmployerId)
      givenEmployerExternalIDNotExist(invalidEmployerId)

      val exception = assertFailsWith<Exception> { jobRegistrar.registerCreation(job) }

      assertThat(exception.message)
        .startsWith("Fail to register job-creation").contains("jobId=${job.id}")
      assertThat(exception.cause)
        .isNotNull.isInstanceOfAny(AssertionError::class.java)
        .message().startsWith("Employer external ID is not found")
    }
  }

  @Nested
  @DisplayName("Given an existing job to be updated to MN")
  inner class GivenAJobToUpdate {
    private val job = amazonForkliftOperator.run { copy(description = "$description |updated") }
    private val employerExternalId = 2002L
    private val externalId = 101L
    private val mnJob = job.mnJob(employerExternalId, externalId)
    private val updateJobRequest = UpdateJobRequest.from(mnJob)

    @Test
    fun `update a registered job`() {
      givenAJobToUpdate(job, mnJob)

      jobRegistrar.registerUpdate(job)

      verify(mnJobBoardApiClient).updateJob(updateJobRequest)
    }

    @Test
    fun `throw exception, when external ID has been changed unexpectedly`() {
      givenAJobToUpdate(job, mnJob, mnJob.copy(id = 999L))

      val exception = assertFailsWith<Exception> {
        jobRegistrar.registerUpdate(job)
      }

      with(exception) {
        assertThat(message).startsWith("Fail to register job-update").contains("jobId=${job.id}")
        with(cause!!) {
          assertThat(this).isInstanceOfAny(AssertionError::class.java)
          assertThat(message).startsWith("MN Job ID has changed!").contains("jobId=${job.id}")
        }
      }
    }

    @Test
    fun `throw exception, with error from conversion`() {
      val expectedException = IllegalStateException("Job with id=${job.id} not found (ID mapping missing)")
      whenever(jobExternalIdRepository.findByKeyId(job.id)).thenReturn(null)

      val actualException = assertFailsWith<Exception> {
        jobRegistrar.registerUpdate(job)
      }

      with(actualException) {
        assertThat(message).startsWith("Fail to register job-update").contains("jobId=${job.id}")
        with(cause!!) {
          assertThat(this).isInstanceOfAny(expectedException.javaClass)
          assertThat(message).isEqualTo(expectedException.message)
        }
      }
    }

    @Test
    fun `throw exception, with error from updating downstream system`() {
      givenAJobToUpdate(job, mnJob)
      val expectedException = "some errors while updating".let {
        RuntimeException("Fail to update job! errorResponse=$it", RuntimeException(it))
      }
      whenever(mnJobBoardApiClient.updateJob(updateJobRequest)).thenThrow(expectedException)

      val actualException = assertFailsWith<Throwable> {
        jobRegistrar.registerUpdate(job)
      }

      with(actualException) {
        assertThat(message).startsWith("Fail to register job-update").contains("jobId=${job.id}")
        with(cause!!) {
          assertThat(message).isEqualTo(expectedException.message)
        }
      }
    }
  }

  private fun givenAJobToCreate(job: Job, mnJob: MNJob, mnJobCreated: MNJob = mnJob) {
    givenJobExternalIDNotExist(job.id)
    givenEmployerExternalIDExists(job.employerId, mnJob.employerId)
    givenRefDataMappings(job)
    whenever(mnJobBoardApiClient.createJob(CreateJobRequest.from(mnJob))).thenReturn(mnJobCreated)
  }

  private fun givenAJobToUpdate(job: Job, mnJob: MNJob, mnJobUpdated: MNJob = mnJob) {
    givenJobExternalIDExists(job.id, mnJob.id!!)
    givenEmployerExternalIDExists(job.employerId, mnJob.employerId)
    givenRefDataMappings(job)
    whenever(mnJobBoardApiClient.updateJob(UpdateJobRequest.from(mnJob))).thenReturn(mnJobUpdated)
  }

  private fun givenJobExternalIDExists(id: String, externalId: Long) {
    val jobExternalId = JobExternalId(id, externalId)
    whenever(jobExternalIdRepository.findByKeyId(id)).thenReturn(jobExternalId)
  }

  private fun givenJobExternalIDNotExist(id: String) {
    whenever(jobExternalIdRepository.findByKeyId(id)).thenReturn(null)
  }

  private fun givenEmployerExternalIDExists(id: String, externalId: Long) {
    val employerExternalId = EmployerExternalId(id, externalId)
    whenever(employerExternalIdRepository.findByKeyId(id)).thenReturn(employerExternalId)
  }

  private fun givenEmployerExternalIDNotExist(id: String) {
    whenever(employerExternalIdRepository.findByKeyId(id)).thenReturn(null)
  }

  private fun givenRefDataMappings(job: Job) = job.run {
    givenRefDataMappings(
      offenceExclusions = offenceExclusions.split(","),
      jobSources = listOfNotNull(sourcePrimary, job.sourceSecondary),
      employerSector = industrySector,
      workPattern = workPattern,
      contractType = contractType,
      hoursPerWeek = hoursPerWeek,
      salaryPeriod = salaryPeriod,
      baseLocation = baseLocation,
    )
  }

  private fun givenRefDataMappings(
    offenceExclusions: List<String>,
    jobSources: List<String>,
    employerSector: String,
    workPattern: String,
    contractType: String,
    hoursPerWeek: String,
    salaryPeriod: String,
    baseLocation: String?,
  ) {
    offenceExclusions.forEach { givenRefDataIdMapping(OFFENCE_EXCLUSION.type, it) }
    jobSources.forEach { givenRefDataIdMapping(JOB_SOURCE.type, it) }

    givenRefDataIdMapping(EMPLOYER_SECTOR.type, employerSector)
    givenRefDataIdMapping(WORK_PATTERN.type, workPattern)
    givenRefDataIdMapping(CONTRACT_TYPE.type, contractType)
    givenRefDataIdMapping(HOURS_PER_WEEK.type, hoursPerWeek)
    givenRefDataIdMapping(SALARY_PERIOD.type, salaryPeriod)

    baseLocation?.let { givenRefDataIdMapping(BASE_LOCATION.type, baseLocation) }
  }
}
