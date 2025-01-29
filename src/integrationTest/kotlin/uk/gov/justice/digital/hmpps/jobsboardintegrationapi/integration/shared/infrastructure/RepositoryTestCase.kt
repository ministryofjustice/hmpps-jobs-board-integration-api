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

    whenever(dateTimeProvider.now).thenAnswer { Optional.of(currentTime) }
  }

  protected fun randomId() = UUID.randomUUID().toString()
  protected fun randomExtId() = SecureRandom().nextLong()

  private val refDataMappingsForTests: List<RefDataMapping>
    get() = mapOf(
      "employer_status" to mapOf(
        "KEY_PARTNER" to 1L,
        "GOLD" to 2,
        "SILVER" to 3,
      ),
      "employer_sector" to mapOf(
        "ADMIN_SUPPORT" to 14L,
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
    ).map { (refData, mapping) -> mapping.map { (value, externalId) -> RefDataMapping(refData, value, externalId) } }
      .flatten()
}

@Repository
internal interface RefDataMappingTestOnlyRepository : JpaRepository<RefDataMapping, RefDataMappingKey>
