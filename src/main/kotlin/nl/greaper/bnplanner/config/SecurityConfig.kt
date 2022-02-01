package nl.greaper.bnplanner.config

import nl.greaper.bnplanner.auth.JwtTokenFilter
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val tokenFilter: JwtTokenFilter
) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable()

        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

        http.exceptionHandling()
            .authenticationEntryPoint { request: HttpServletRequest?, response: HttpServletResponse, ex: AuthenticationException ->
                response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ex.message
                )
            }
            .and()

        http.authorizeRequests()
            .anyRequest().authenticated()

        http.addFilterBefore(
            tokenFilter,
            UsernamePasswordAuthenticationFilter::class.java
        )
    }
}