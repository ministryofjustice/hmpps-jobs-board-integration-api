package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.application

import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.config.SqsTestConfig
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalId
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure.INTEGRATION_QUEUE_ID
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration

@Import(SqsTestConfig::class)
abstract class ApplicationTestCase : IntegrationTestBase() {
  @Autowired
  protected lateinit var employerExternalIdRepository: EmployerExternalIdRepository

  @Autowired
  protected lateinit var jobExternalIdRepository: JobExternalIdRepository

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean
  @Qualifier("integrationqueue-sqs-client")
  protected lateinit var integrationQeueSqsClientSpy: SqsAsyncClient

  protected val integrationQueueUrl by lazy { integrationQueue.queueUrl }
  protected val integrationDlqUrl by lazy { integrationQueue.dlqUrl!! }

  private val integrationQueue by lazy {
    hmppsQueueService.findByQueueId(INTEGRATION_QUEUE_ID)
      ?: throw MissingQueueException("HmppsQueue $INTEGRATION_QUEUE_ID not found")
  }

  @BeforeEach
  override fun setUp() {
    super.setUp()
    employerExternalIdRepository.run {
      deleteAll()
      flush()
    }
    jobExternalIdRepository.run {
      deleteAll()
      flush()
    }
  }

  protected fun givenEmployerExternalIds(employerIds: List<String>) = givenEmployerExternalIds(*employerIds.toTypedArray())

  protected fun givenEmployerExternalIds(vararg employerIds: String) = employerIds.mapIndexed { index, id ->
    val extId = index + 1L
    employerExternalIdRepository.save(EmployerExternalId(id, extId))
    extId
  }.also { employerExternalIdRepository.flush() }.last()

  protected fun givenJobExternalIds(jobIds: List<String>) = givenJobExternalIds(*jobIds.toTypedArray())

  protected fun givenJobExternalIds(vararg jobIds: String) = jobIds.mapIndexed { index, id ->
    val extId = index + 1L
    jobExternalIdRepository.save(JobExternalId(id, extId))
    extId
  }.also { jobExternalIdRepository.flush() }.last()

  protected fun awaitIntegrationQueue(messageCount: Int = 0) {
    await untilCallTo { integrationQueueMessageCount() } matches { it == messageCount }
  }

  protected fun awaitIntegrationQueueAllMessagesAreGone(timeoutInSecond: Long) = awaitIntegrationQueueAllMessagesAreGone(Duration.ofSeconds(timeoutInSecond))
  protected fun awaitIntegrationQueueAllMessagesAreGone(timeout: Duration? = null) {
    await.let { timeout?.let { t -> it.timeout(t) } ?: it } untilCallTo {
      integrationQeueSqsClientSpy.countAllMessagesOnQueue(integrationQueueUrl).get()
    } matches { it == 0 }
  }

  protected fun integrationQueueMessageCount() = integrationQeueSqsClientSpy.countMessagesOnQueue(integrationQueueUrl).get()
  protected fun integrationDlqMessageCount() = integrationQeueSqsClientSpy.countMessagesOnQueue(integrationDlqUrl).get()

  protected fun purgeIntegrationQueues() = listOf(integrationQueueUrl, integrationDlqUrl)
    .map { PurgeQueueRequest(integrationQueue.queueName, integrationQeueSqsClientSpy, it) }
    .forEach { runBlocking { hmppsQueueService.purgeQueue(it) } }

  protected fun Employer.makeCopy() = copy(createdAt = timeProvider.nowAsInstant())

  protected fun Job.makeCopy() = let { copy(createdAt = timeProvider.nowAsInstant()).apply { employer = it.employer } }
}
