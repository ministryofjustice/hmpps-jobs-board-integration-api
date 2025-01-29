package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.config

import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
@ComponentScan(basePackages = ["uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration.shared.infrastructure"])
@Profile("test", "test-repo")
class TestJpaConfig {
  @Primary
  @Bean
  fun dateTimeProvider(): DateTimeProvider {
    return mock(DateTimeProvider::class.java)
  }
}
