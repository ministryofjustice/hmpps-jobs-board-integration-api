plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.0.0"
  kotlin("plugin.spring") version "2.2.10"
  `jvm-test-suite`
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.5.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
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
        implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.5.0")
        implementation("org.wiremock:wiremock-standalone:3.13.1")
        implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.31") {
          exclude(group = "io.swagger.core.v3")
          exclude(group = "io.swagger.parser.v3", module = "swagger-parser-v2-converter")
          exclude(group = "io.swagger.parser.v3", module = "swagger-parser-safe-url-resolver")
        }
        implementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
        implementation("org.springframework.boot:spring-boot-testcontainers") {
          implementation("org.apache.commons:commons-compress:1.27.1")
        }
        implementation("org.testcontainers:localstack")
        implementation("org.testcontainers:postgresql")
        implementation("org.jetbrains.kotlin:kotlin-test-junit5")
        implementation("org.awaitility:awaitility-kotlin")
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
