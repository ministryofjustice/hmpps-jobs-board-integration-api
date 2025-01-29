package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.ExternalIdKey

@Repository
interface EmployerExternalIdRepository : JpaRepository<EmployerExternalId, ExternalIdKey> {
  fun findByKeyId(id: String): EmployerExternalId?
  fun findByKeyExternalId(externalId: Long): EmployerExternalId?
}
