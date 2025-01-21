package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.AliasFor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@ConditionalOnProperty("api.integration.enabled")
annotation class ConditionalOnIntegrationEnabled(
  @get:AliasFor(
    annotation = ConditionalOnProperty::class,
    attribute = "havingValue",
  ) val value: String = "true",
)
