package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain

import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import java.util.*

@NoRepositoryBean
interface ReadOnlyRepository<T, ID> : Repository<T & Any, ID & Any> {
  fun findById(id: ID): Optional<T>
  fun findAll(): List<T>?
}
