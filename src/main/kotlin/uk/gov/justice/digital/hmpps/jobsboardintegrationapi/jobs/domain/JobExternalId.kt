package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.Auditable
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.ExternalIdKey

@Entity
@Table(name = "jobs_ext_ids")
data class JobExternalId(
  @EmbeddedId var key: ExternalIdKey,
) : Auditable() {
  constructor(id: String, externalId: Long) : this(ExternalIdKey(id, externalId))
  constructor() : this("", 0)
}
