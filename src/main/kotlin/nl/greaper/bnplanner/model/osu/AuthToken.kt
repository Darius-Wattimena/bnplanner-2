package nl.greaper.bnplanner.model.osu

data class AuthToken(
    val token_type: String,
    val expires_in: Int,
    val access_token: String,
    val refresh_token: String
)
