package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain

import java.time.Instant

data class Employer(
  var id: String,
  val name: String,
  val description: String,
  val sector: String,
  val status: String,
  var createdBy: String? = null,
  var lastModifiedBy: String? = null,
  var createdAt: Instant? = null,
  var lastModifiedAt: Instant? = null,
) {
  override fun toString(): String = """
    Employer(id=$id,
        name=$name,
        description=$description,
        sector=$sector,
        status=$status,
        createdBy=$createdBy,
        createdAt=$createdAt,
        lastModifiedBy=$lastModifiedBy,
        lastModifiedAt=$lastModifiedAt
    )
  """.trimIndent()
}
