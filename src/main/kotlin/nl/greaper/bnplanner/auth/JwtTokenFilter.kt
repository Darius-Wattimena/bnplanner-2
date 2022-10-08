package nl.greaper.bnplanner.auth

import nl.greaper.bnplanner.config.AiessConfig
import nl.greaper.bnplanner.service.UserService
import nl.greaper.bnplanner.util.getAiessRole
import nl.greaper.bnplanner.util.getHighestRoleForUser
import nl.greaper.bnplanner.util.parseJwtToken
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtTokenFilter(
    private val aiessConfig: AiessConfig,
    private val userService: UserService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        // Get authorization header and validate
        val header = request.getHeader(AUTHORIZATION)
        if (header.isNullOrBlank()) {
            chain.doFilter(request, response)
            return
        }

        when {
            header.startsWith("Aiess ") -> {
                val token = header.split(" ".toRegex()).toTypedArray().getOrNull(1)?.trim()

                if (aiessConfig.token == token) {
                    val permission = getAiessRole()

                    val authentication = UsernamePasswordAuthenticationToken(
                        permission, token, permission.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    )

                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                    chain.doFilter(request, response)
                    return
                }
            }
            header.startsWith("Bearer ") -> {
                // Get jwt token and validate
                val token = header.split(" ".toRegex()).toTypedArray().getOrNull(1)?.trim()
                if (token == null) {
                    chain.doFilter(request, response)
                    return
                }

                val claims = parseJwtToken(token)

                if (claims == null) {
                    chain.doFilter(request, response)
                    return
                }

                val osuId = claims.subject
                val user = userService.findUserById(token, osuId)

                if (user == null) {
                    // Only should end up here if the user that is trying to log in is restricted and never saved in the database
                    chain.doFilter(request, response)
                    return
                }

                val permission = getHighestRoleForUser(user)

                val authentication = UsernamePasswordAuthenticationToken(
                    permission, token, permission.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                )

                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
                chain.doFilter(request, response)
                return
            }
        }

        chain.doFilter(request, response)
        return
    }
}
