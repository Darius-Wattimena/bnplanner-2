import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("fi.evident.beanstalk") version "0.3.3"
    war
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.spring") version "2.1.20"
}

group = "nl.greaper"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
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
    implementation("org.litote.kmongo:kmongo:5.2.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging:7.0.7")
    implementation("ch.qos.logback:logback-classic")
    implementation("org.codehaus.janino:janino")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    // Discord
    implementation("net.dv8tion:JDA:4.4.0_350") {
        exclude("opus-java")
    }

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
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
