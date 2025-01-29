package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ExternalIdKey(
  @Column(name = "id", unique = true) val id: String,
  @Column(name = "ext_id", unique = true) val externalId: Long,
) {
  constructor() : this("", 0)
}
