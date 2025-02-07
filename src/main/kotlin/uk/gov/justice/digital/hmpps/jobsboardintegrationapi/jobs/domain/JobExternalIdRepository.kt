package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.ExternalIdKey

@Repository
interface JobExternalIdRepository : JpaRepository<JobExternalId, ExternalIdKey> {
  fun findByKeyId(id: String): JobExternalId?
  fun findByKeyExternalId(externalId: Long): JobExternalId?
}
