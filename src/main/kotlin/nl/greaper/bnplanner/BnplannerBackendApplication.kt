package nl.greaper.bnplanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableGlobalMethodSecurity(jsr250Enabled = true)
class BnplannerBackendApplication

fun main(args: Array<String>) {
    runApplication<BnplannerBackendApplication>(*args)
}
