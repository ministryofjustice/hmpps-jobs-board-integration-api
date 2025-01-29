package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.Auditable
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.ExternalIdKey

@Entity
@Table(name = "employers_ext_ids")
data class EmployerExternalId(
  @EmbeddedId var key: ExternalIdKey,
) : Auditable() {
  constructor(id: String, externalId: Long) : this(ExternalIdKey(id, externalId))
  constructor() : this("", 0)
}
