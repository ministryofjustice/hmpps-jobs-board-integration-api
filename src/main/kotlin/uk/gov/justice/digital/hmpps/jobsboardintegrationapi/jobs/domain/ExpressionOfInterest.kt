package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain

data class ExpressionOfInterest(
  val jobId: String,
  val prisonNumber: String,
) {
  private var _job: Job? = null

  constructor(jobId: String, prisonNumber: String, job: Job? = null) : this(jobId, prisonNumber) {
    this.job = job
  }

  var job: Job?
    get() = _job
    set(job) {
      job?.let { if (job.id != jobId) throw IllegalArgumentException("Job ID did not match") }
      _job = job
    }

  override fun toString(): String = "ExpressionOfInterest(jobId=$jobId, prisonNumber=$prisonNumber)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ExpressionOfInterest

    if (jobId != other.jobId) return false
    if (prisonNumber != other.prisonNumber) return false

    return true
  }

  override fun hashCode(): Int {
    var result = jobId.hashCode()
    result = 31 * result + prisonNumber.hashCode()
    return result
  }
}
