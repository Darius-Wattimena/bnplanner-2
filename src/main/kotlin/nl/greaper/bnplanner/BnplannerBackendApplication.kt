package nl.greaper.bnplanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BnplannerBackendApplication

fun main(args: Array<String>) {
    runApplication<BnplannerBackendApplication>(*args)
}
