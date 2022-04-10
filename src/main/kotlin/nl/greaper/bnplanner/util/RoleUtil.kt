package nl.greaper.bnplanner.util

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User

fun getHighestRole(osuRoles: Set<Role>): Role {
    return when {
        Role.NominationAssessment in osuRoles -> Role.NominationAssessment
        Role.Nominator in osuRoles -> Role.Nominator
        Role.Probation in osuRoles -> Role.Probation
        Role.Loved in osuRoles -> Role.Loved
        else -> Role.Mapper
    }
}

fun getRolePermissions(osuRole: Role): RolePermission {
    val permissions = when (osuRole) {
        Role.Mapper, Role.Loved -> setOf(RolePermission.VIEWER)
        Role.Nominator, Role.Probation -> setOf(RolePermission.VIEWER, RolePermission.EDITOR)
        Role.NominationAssessment -> setOf(RolePermission.ADMIN, RolePermission.VIEWER, RolePermission.EDITOR)
    }

    return RolePermission(osuRole, permissions)
}

fun getAiessRole(): RolePermission {
    return RolePermission(
        Role.NominationAssessment,
        setOf(RolePermission.BOT)
    )
}

fun getHighestRoleForUser(user: User?): RolePermission {
    if (user == null) {
        return getRolePermissions(Role.Mapper)
    }

    if (user.osuId == "2369776") {
        return RolePermission(
            Role.NominationAssessment,
            setOf(RolePermission.DEVELOPER, RolePermission.ADMIN, RolePermission.VIEWER, RolePermission.EDITOR)
        )
    }

    val userRoles = user.gamemodes.map { it.role }.toSet()
    val userRole = getHighestRole(userRoles)

    return getRolePermissions(userRole)
}
