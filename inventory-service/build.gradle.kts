import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "com.areg.ticketing"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        // Functional tests: the service as a whole with a real Postgres in Docker (source set src/functionalTest)
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(platform(SpringBootPlugin.BOM_COORDINATES))
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.testcontainers:testcontainers-junit-jupiter")
                implementation("org.testcontainers:testcontainers-postgresql")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["implementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

tasks.check {
    dependsOn(testing.suites.named("functionalTest"))
}
