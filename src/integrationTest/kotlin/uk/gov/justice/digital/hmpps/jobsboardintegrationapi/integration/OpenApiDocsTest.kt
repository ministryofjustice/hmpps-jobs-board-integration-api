package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.integration

import io.swagger.v3.parser.OpenAPIV3Parser
import net.minidev.json.JSONArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OpenApiDocsTest : IntegrationTestBase() {
  @LocalServerPort
  private val port: Int = 0

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html?configUrl=/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `open api docs redirect to correct page`() {
    webTestClient.get()
      .uri("/swagger-ui.html")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().is3xxRedirection
      .expectHeader().value("Location") { it.contains("/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config") }
  }

  @Test
  fun `the open api json contains documentation`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("paths").isNotEmpty
  }

  @Test
  fun `the open api json contains the version number`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }

  @Test
  fun `the open api json is valid and contains documentation`() {
    val result = OpenAPIV3Parser().readLocation("http://localhost:$port/v3/api-docs", null, null)
    assertThat(result.messages).isEmpty()
    assertThat(result.openAPI.paths).isNotEmpty
  }

  @Test
  fun `the open api json path security requirements are valid`() {
    val result = OpenAPIV3Parser().readLocation("http://localhost:$port/v3/api-docs", null, null)

    // TODO remove these exclusions when SQS Queue Admin endpoints have been enhanced to include Security reqs.
    val exclusions = setOf(
      "/queue-admin/retry-dlq/{dlqName}",
      "/queue-admin/retry-all-dlqs",
      "/queue-admin/purge-queue/{queueName}",
      "/queue-admin/get-dlq-messages/{dlqName}",
    )

    // The security requirements of each path don't appear to be validated like they are at https://editor.swagger.io/
    // We therefore need to grab all the valid security requirements and check that each path only contains those items
    val securityRequirements = result.openAPI.security.flatMap { it.keys }
    result.openAPI.paths
      .filter { !exclusions.contains(it.key) }
      .forEach { pathItem ->
        assertThat(pathItem.value.get).isNotNull
        assertThat(pathItem.value.get.security.flatMap { it.keys }).isSubsetOf(securityRequirements)
      }
  }

  @ParameterizedTest
  @CsvSource(value = ["jobs-board-integration-api-queue-admin-role, ROLE_QUEUE_ADMIN"])
  fun `the security scheme is setup for bearer tokens`(key: String, role: String) {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.components.securitySchemes.$key.type").isEqualTo("http")
      .jsonPath("$.components.securitySchemes.$key.scheme").isEqualTo("bearer")
      .jsonPath("$.components.securitySchemes.$key.description").value<String> {
        assertThat(it).contains(role)
      }
      .jsonPath("$.components.securitySchemes.$key.bearerFormat").isEqualTo("JWT")
      .jsonPath("$.security[0].$key").isEqualTo(JSONArray().apply { this.addAll(listOf("read", "write")) })
  }

  @Test
  fun `all endpoints have a security scheme defined`() {
    // There are 4 SQS endpoints without security scheme, to be excluded from this test; all these endpoint has single tag "hmpps-queue-resource"
    val queueAdminTag = "hmpps-queue-resource"
    val queueAdminEndpointCount = 4

    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paths[*][*][?(!@.security)]..tags[0]").value<JSONArray> {
        assertThat(it).hasSize(queueAdminEndpointCount)
        it.forEach { tag -> assertThat(tag).isEqualTo(queueAdminTag) }
      }
  }
}
