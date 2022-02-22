package nl.greaper.bnplanner.auth

import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.service.UserService
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
    private val userService: UserService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        // Get authorization header and validate
        val header = request.getHeader(AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response)
            return
        }

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
        val user = userService.findUserFromId(token, osuId)

        if (user == null) {
            // Only should end up here if the user that is trying to log in is restricted and never saved in the database
            chain.doFilter(request, response)
            return
        }

        val permission = if (user.osuId == "2369776") {
            RolePermission(
                Role.NominationAssessment,
                setOf(RolePermission.DEVELOPER, RolePermission.ADMIN, RolePermission.VIEWER, RolePermission.EDITOR)
            )
        } else {
            val userRoles = user.gamemodes.map { it.role }.toSet()
            val userRole = getHighestRole(userRoles)

            getRolePermissions(userRole)
        }

        val authentication = UsernamePasswordAuthenticationToken(
            permission, token, permission.roles.map { SimpleGrantedAuthority("ROLE_$it") }
        )

        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(request, response)
    }

    private fun getHighestRole(osuRoles: Set<Role>): Role {
        return when {
            Role.NominationAssessment in osuRoles -> Role.NominationAssessment
            Role.Nominator in osuRoles -> Role.Nominator
            Role.Probation in osuRoles -> Role.Probation
            Role.Loved in osuRoles -> Role.Loved
            else -> Role.Mapper
        }
    }

    private fun getRolePermissions(osuRole: Role): RolePermission {
        val permissions = when (osuRole) {
            Role.Mapper, Role.Loved -> setOf(RolePermission.VIEWER)
            Role.Nominator, Role.Probation -> setOf(RolePermission.VIEWER, RolePermission.EDITOR)
            Role.NominationAssessment -> setOf(RolePermission.ADMIN, RolePermission.VIEWER, RolePermission.EDITOR)
        }

        return RolePermission(osuRole, permissions)
    }
}