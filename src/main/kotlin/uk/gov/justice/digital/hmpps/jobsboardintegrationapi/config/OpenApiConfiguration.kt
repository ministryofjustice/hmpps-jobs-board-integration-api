package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://jobs-board-integration-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://jobs-board-integration-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://jobs-board-integration-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    ).tags(
      listOf(
        Tag().name("Resend data").description("Resending current data to external party"),
      ),
    ).info(
      Info().title("HMPPS Jobs Board Integration Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "jobs-board-integration-api-queue-admin-role",
        SecurityScheme().addBearerJwtRequirement("ROLE_QUEUE_ADMIN"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("jobs-board-integration-api-queue-admin-role", listOf("read", "write")))
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
  .scheme("bearer")
  .bearerFormat("JWT")
  .`in`(SecurityScheme.In.HEADER)
  .name("Authorization")
  .description("A HMPPS Auth access token with the `$role` role.")
