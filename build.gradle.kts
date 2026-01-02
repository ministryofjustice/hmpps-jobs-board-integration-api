plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.0-beta-4"
  kotlin("plugin.spring") version "2.3.0"
  `jvm-test-suite`
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0-beta-3")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0-beta-2")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
  jvmToolchain(21)
}

testing {
  suites {
    @Suppress("UnstableApiUsage")
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }

    @Suppress("UnstableApiUsage")
    register<JvmTestSuite>("integrationTest") {
      dependencies {
        kotlin.target.compilations { named("integrationTest") { associateWith(getByName("main")) } }
        implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0-beta-3")
        implementation("org.wiremock:wiremock-standalone:3.13.2")
        implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.37") {
          exclude(group = "io.swagger.core.v3")
          exclude(group = "io.swagger.parser.v3", module = "swagger-parser-v2-converter")
          exclude(group = "io.swagger.parser.v3", module = "swagger-parser-safe-url-resolver")
        }
        implementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
        implementation("org.springframework.boot:spring-boot-testcontainers")

        implementation("org.testcontainers:testcontainers-localstack")
        implementation("org.testcontainers:testcontainers-postgresql")
        implementation("org.jetbrains.kotlin:kotlin-test-junit5")
        implementation("org.awaitility:awaitility-kotlin")

        implementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
        implementation("org.springframework.boot:spring-boot-starter-webclient-test")
        implementation("org.springframework.boot:spring-boot-webtestclient")
      }

      targets {
        all {
          testTask.configure {
            shouldRunAfter(test)
          }
        }
      }
    }
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }

  check {
    dependsOn(named("test"), named("integrationTest"))
  }
}

dependencyCheck {
  suppressionFiles.add("jobs-board-integration-suppressions.xml")
}
