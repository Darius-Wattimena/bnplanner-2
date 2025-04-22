package nl.greaper.bnplanner.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nl.greaper.bnplanner.auth.JwtTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val tokenFilter: JwtTokenFilter
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http.cors {  }
            .csrf { configurer -> configurer.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { configurer ->
                configurer.authenticationEntryPoint { _: HttpServletRequest?, response: HttpServletResponse, ex: AuthenticationException ->
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ex.message
                    )
                }
            }
            .authorizeHttpRequests { registry ->
                registry.requestMatchers("/v2/auth", "/v2/auth/refresh", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(
        cors: CorsProperties
    ): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = cors.uris.split(",")
        configuration.allowedMethods = cors.methods.split(",")
        configuration.allowedHeaders = cors.headers.split(",")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
