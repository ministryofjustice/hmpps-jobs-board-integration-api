package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.refdata.domain

enum class RefData(val type: String) {
  EMPLOYER_STATUS("employer_status"),
  EMPLOYER_SECTOR("employer_sector"),
  JOB_SOURCE("job_source"),
  SALARY_PERIOD("salary_period"),
  WORK_PATTERN("work_pattern"),
  CONTRACT_TYPE("contract_type"),
  HOURS_PER_WEEK("hours_per_week"),
  BASE_LOCATION("base_location"),
  OFFENCE_EXCLUSION("offence_exclusion"),
}
