package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.EmployerExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.config.TestJpaConfig
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.testcontainers.PostgresContainer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.JobExternalIdRepository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMapping
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingKey
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingRepository
import java.security.SecureRandom
import java.time.Instant
import java.util.*

@DataJpaTest
@Import(TestJpaConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-repo")
abstract class RepositoryTestCase {
  @Autowired
  protected lateinit var dateTimeProvider: DateTimeProvider

  @Autowired
  protected lateinit var refDataMappingRepository: RefDataMappingRepository

  @Autowired
  protected lateinit var employerExternalIdRepository: EmployerExternalIdRepository

  @Autowired
  protected lateinit var jobExternalIdRepository: JobExternalIdRepository

  @Autowired
  private lateinit var refDataMappingTestOnlyRepository: RefDataMappingTestOnlyRepository

  protected final val defaultCurrentTime: Instant = Instant.parse("2025-01-01T00:00:00.00Z")

  protected val currentTime: Instant get() = defaultCurrentTime

  companion object {
    private val postgresContainer = PostgresContainer.repositoryContainer

    @JvmStatic
    @DynamicPropertySource
    fun configureTestContainers(registry: DynamicPropertyRegistry) {
      postgresContainer?.run {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
        registry.add("spring.datasource.username", postgresContainer::getUsername)
        registry.add("spring.datasource.password", postgresContainer::getPassword)
      }
    }
  }

  @BeforeAll
  fun setUpClass() {
    refDataMappingTestOnlyRepository.saveAll(refDataMappingsForTests)
  }

  @BeforeEach
  fun setUp() {
    employerExternalIdRepository.deleteAll()
    jobExternalIdRepository.deleteAll()

    whenever(dateTimeProvider.now).thenAnswer { Optional.of(currentTime) }
  }

  protected fun randomId() = UUID.randomUUID().toString()
  protected fun randomExtId() = SecureRandom().nextLong()

  private val refDataMappingsForTests: List<RefDataMapping>
    get() = mapOf(
      "employer_status" to mapOf(
        "KEY_PARTNER" to 1,
        "GOLD" to 2,
        "SILVER" to 3,
      ),
      "employer_sector" to mapOf(
        "ADMIN_SUPPORT" to 14,
        "AGRICULTURE" to 1,
        "ARTS_ENTERTAINMENT" to 18,
        "CONSTRUCTION" to 6,
        "EDUCATION" to 16,
        "ENERGY" to 4,
        "FINANCE" to 11,
        "HEALTH_SOCIAL" to 17,
        "HOSPITALITY_CATERING" to 9,
        "LOGISTICS" to 8,
        "MANUFACTURING" to 3,
        "MINING" to 2,
        "OTHER" to 19,
        "PROFESSIONALS_SCIENTISTS_TECHNICIANS" to 13,
        "PROPERTY" to 12,
        "PUBLIC_ADMIN_DEFENCE" to 15,
        "WASTE_MANAGEMENT" to 5,
        "RETAIL" to 7,
        "TECHNOLOGY" to 10,
      ),
      "job_source" to mapOf(
        "DWP" to 4,
        "EAB" to 14,
        "EDUCATION" to 15,
        "IAG" to 8,
        "NFN" to 1,
        "PRISON" to 16,
        "THIRD_SECTOR" to 10,
        "PEL" to 2,
        "OTHER" to 11,
      ),
      "salary_period" to mapOf(
        "PER_DAY" to 2,
        "PER_FORTNIGHT" to 4,
        "PER_HOUR" to 1,
        "PER_MONTH" to 5,
        "PER_WEEK" to 3,
        "PER_YEAR" to 6,
        "PER_YEAR_PRO_RATA" to 7,
      ),
      "work_pattern" to mapOf(
        "ANNUALISED_HOURS" to 1,
        "COMPRESSED_HOURS" to 2,
        "FLEXI_TIME" to 3,
        "FLEXIBLE_SHIFTS" to 4,
        "JOB_SHARE" to 5,
        "STAGGERED_HOURS" to 6,
        "TERM_TIME_HOURS" to 7,
        "UNSOCIABLE_HOURS" to 8,
      ),
      "contract_type" to mapOf(
        "FIXED_TERM_CONTRACT" to 4,
        "PERMANENT" to 1,
        "SELF_EMPLOYMENT" to 3,
        "TEMPORARY" to 2,
      ),
      "hours_per_week" to mapOf(
        "FULL_TIME" to 2,
        "FULL_TIME_40_PLUS" to 1,
        "PART_TIME" to 3,
        "ZERO_HOURS" to 4,
      ),
      "base_location" to mapOf(
        "REMOTE" to 1,
        "HYBRID" to 3,
        "WORKPLACE" to 2,
      ),
      "offence_exclusion" to mapOf(
        "NONE" to 1,
        "CASE_BY_CASE" to 15,
        "ARSON" to 16,
        "DRIVING" to 17,
        "MURDER" to 18,
        "SEXUAL" to 3,
        "TERRORISM" to 19,
        "OTHER" to 14,
      ),
    ).map { (refData, mapping) -> mapping.map { (value, externalId) -> RefDataMapping(refData, value, externalId) } }
      .flatten()
}

@Repository
internal interface RefDataMappingTestOnlyRepository : JpaRepository<RefDataMapping, RefDataMappingKey>
