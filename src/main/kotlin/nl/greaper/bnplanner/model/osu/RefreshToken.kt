package nl.greaper.bnplanner.model.osu

data class RefreshToken(
    val grant_type: String,
    val client_id: Int,
    val client_secret: String,
    val refresh_token: String
)
