plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.2"
  kotlin("plugin.spring") version "2.0.21"
  `jvm-test-suite`
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

ext["logback.version"] = "1.5.15"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
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
        implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.1.1")
        implementation("org.wiremock:wiremock-standalone:3.9.2")
        implementation("io.swagger.parser.v3:swagger-parser:2.1.24") {
          exclude(group = "io.swagger.core.v3")
        }
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
