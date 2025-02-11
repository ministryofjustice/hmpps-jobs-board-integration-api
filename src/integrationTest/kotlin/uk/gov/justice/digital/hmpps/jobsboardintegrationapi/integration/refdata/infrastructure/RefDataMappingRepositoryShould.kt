package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.refdata.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure.RepositoryTestCase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain.RefDataMappingKey
import kotlin.test.assertTrue

class RefDataMappingRepositoryShould : RepositoryTestCase() {
  @Test
  fun `return non empty list of reference data mapping(s)`() {
    val refDataMappings = this.refDataMappingRepository.findAll()
    assertThat(refDataMappings).isNotEmpty
  }

  @Test
  fun `return nothing for undefined reference data`() {
    val unknownRefKey = RefDataMappingKey()
    val refDataMapping = this.refDataMappingRepository.findById(unknownRefKey)

    assertTrue(refDataMapping.isEmpty, "Nothing should be found!")
  }

  @Test
  fun `return correct mapping, given reference data and data value`() {
    val refData = "employer_status"
    val dataValue = "GOLD"
    val expectedExtId = 2

    val mapping = refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(refData, dataValue)

    assertThat(mapping).isNotNull
    assertThat(mapping!!.data.externalId).isEqualTo(expectedExtId)
  }

  @Test
  fun `return correct mapping, given reference data and data external ID`() {
    val refData = "employer_sector"
    val dataExternalId = 6
    val expectedDataValue = "CONSTRUCTION"

    val mapping = refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataExternalId(refData, dataExternalId)
    assertThat(mapping).isNotNull
    assertThat(mapping!!.data.value).isEqualTo(expectedDataValue)
  }

  @Test
  fun `return correct mapping, regardless of case`() {
    val refData = "EMPLOYER_STATUS"
    val dataValue = "gold"
    val expectedExtId = 2

    val mapping = refDataMappingRepository.findByDataRefDataIgnoreCaseAndDataValueIgnoreCase(refData, dataValue)

    assertThat(mapping).isNotNull
    assertThat(mapping!!.data.externalId).isEqualTo(expectedExtId)
  }

  @Nested
  @DisplayName("Given reference data mappings for employer")
  inner class GivenRefDataMappingsForEmployer {
    @Test
    fun `return mappings of Employer Status`() = assertRefDataMappingsHasSize("employer_status", 3)

    @Test
    fun `return mappings of Employer Sector`() = assertRefDataMappingsHasSize("employer_sector", 19)
  }

  @Nested
  @DisplayName("Given reference data mappings for job")
  inner class GivenRefDataMappingsForJob {
    @Test
    fun `return mappings of Industry Sector`() = assertRefDataMappingsHasSize("employer_sector", 19)

    @Test
    fun `return mappings of Job Source`() = assertRefDataMappingsHasSize("job_source", 9)

    @Test
    fun `return mappings of Salary Period`() = assertRefDataMappingsHasSize("salary_period", 7)

    @Test
    fun `return mappings of Work Pattern`() = assertRefDataMappingsHasSize("work_pattern", 8)

    @Test
    fun `return mappings of Hours per Week`() = assertRefDataMappingsHasSize("hours_per_week", 4)

    @Test
    fun `return mappings of Contract Type`() = assertRefDataMappingsHasSize("contract_type", 4)

    @Test
    fun `return mappings of Offence Exclusion`() = assertRefDataMappingsHasSize("offence_exclusion", 8)
  }

  private fun assertRefDataMappingsHasSize(refData: String, expectedSize: Int) {
    val refDataMappings = this.refDataMappingRepository.findByDataRefData(refData)
    assertThat(refDataMappings).hasSize(expectedSize)
  }
}
