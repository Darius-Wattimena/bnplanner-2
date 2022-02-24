package nl.greaper.bnplanner.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import nl.greaper.bnplanner.model.Role
import java.security.Principal

data class RolePermission(
    val osuRole: Role,
    val roles: Set<String>
): Principal {
    @JsonIgnore
    override fun getName(): String = osuRole.name

    companion object {
        const val DEVELOPER = "DEVELOPER"
        const val ADMIN = "ADMIN"
        const val EDITOR = "EDITOR"
        const val VIEWER = "VIEWER"
    }
}