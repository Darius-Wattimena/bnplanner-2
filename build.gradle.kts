import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("fi.evident.beanstalk") version "0.3.3"
    war
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
}

group = "nl.greaper"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Mongo
    implementation("org.litote.kmongo:kmongo:4.5.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.codehaus.janino:janino:3.1.6")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    beanstalk {
        s3Endpoint = "s3-eu-west-1.amazonaws.com"
        beanstalkEndpoint = "elasticbeanstalk.eu-west-1.amazonaws.com"

        deployments {
            create("production") {
                file = bootWar
                application = "bnplanner"
                environment = "bnplanner-env"
            }
        }
    }
}
