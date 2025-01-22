package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

data class CreatEmployerRequest(
  val employerName: String,
  val employerBio: String,
  val sectorId: Int,
  val partnerId: Int,
  val imgName: String? = null,
  val path: String? = null,
) {
  companion object {
    fun from(employer: MNEmployer) =
      employer.run { CreatEmployerRequest(employerName, employerBio, sectorId, partnerId, imgName, path) }
  }
}

typealias CreatEmployerResponse = MNEmployer

data class MNCreatEmployerResponse(
  val message: MNMessage,
  val responseObject: MNEmployer,
)

data class MNEmployer(
  val id: Long? = null,
  val employerName: String,
  val employerBio: String,
  val sectorId: Int,
  val partnerId: Int,
  val imgName: String? = null,
  val path: String? = null,
)

data class MNMessage(
  val successCode: String,
  val successMessage: String,
  val httpStatusCode: Int,
)
