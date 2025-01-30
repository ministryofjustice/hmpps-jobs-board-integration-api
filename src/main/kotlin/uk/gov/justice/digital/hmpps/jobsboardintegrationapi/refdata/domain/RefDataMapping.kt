package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.ReadOnlyRepository

@Entity
@Table(name = "ref_data_mappings")
data class RefDataMapping(
  @EmbeddedId var data: RefDataMappingKey,
) {
  constructor(refData: String, value: String, externalId: Int) : this(RefDataMappingKey(refData, value, externalId))
  constructor() : this(RefDataMappingKey())
}

@Embeddable
data class RefDataMappingKey(
  @Column(name = "ref_data") val refData: String,
  @Column(name = "value") val value: String,
  @Column(name = "ext_id") val externalId: Int,
) {
  constructor() : this("", "", 0)
}

@Repository
interface RefDataMappingRepository : ReadOnlyRepository<RefDataMapping, RefDataMappingKey> {
  fun findByDataRefData(refData: String): List<RefDataMapping>

  fun findByDataRefDataAndDataValue(refData: String, dataValue: String): RefDataMapping?

  fun findByDataRefDataAndDataExternalId(refData: String, dataExternalId: Int): RefDataMapping?
}
