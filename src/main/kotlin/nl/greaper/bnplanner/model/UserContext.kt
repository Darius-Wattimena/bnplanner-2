package nl.greaper.bnplanner.model

import nl.greaper.bnplanner.auth.RolePermission

data class UserContext(
    val user: User?,
    val accessToken: String,
    val refreshToken: String,
    val validUntilEpochMilli: Long,
    val permission: RolePermission
)
