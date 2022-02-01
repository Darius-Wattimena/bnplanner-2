package nl.greaper.bnplanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableGlobalMethodSecurity(jsr250Enabled = true)
class BnplannerBackendApplication

fun main(args: Array<String>) {
    runApplication<BnplannerBackendApplication>(*args)
}
