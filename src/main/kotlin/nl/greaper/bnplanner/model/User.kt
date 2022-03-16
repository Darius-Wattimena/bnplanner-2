package nl.greaper.bnplanner.model

import org.bson.codecs.pojo.annotations.BsonId

data class User(
    @BsonId
    val osuId: String,
    val username: String,
    val gamemodes: List<UserGamemode>,
    val restricted: Boolean? = null
)

data class UserGamemode(
    val gamemode: Gamemode,
    val role: Role
)
