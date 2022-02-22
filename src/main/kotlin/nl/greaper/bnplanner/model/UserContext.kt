package nl.greaper.bnplanner.model

data class UserContext(
    val user: User?,
    val accessToken: String,
    val refreshToken: String,
    val validUntilEpochMilli: Long
)
