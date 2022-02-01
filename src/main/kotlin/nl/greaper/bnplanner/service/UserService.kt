package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.UserDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.UserGamemode
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.MeGroup
import org.springframework.stereotype.Service

@Service
class UserService(
    private val dataSource: UserDataSource,
    private val osuHttpClient: OsuHttpClient
) {
    private fun convertOsuUserToUser(osuUser: Me): User {
        return User(
            osuId = osuUser.id,
            username = osuUser.username,
            gamemodes = osuUser.groups?.mapNotNull { osuGroup ->
                if (MeGroup.SupportedGroups.contains(osuGroup.id)) {
                    osuGroup.playmodes?.map { playmode ->
                        UserGamemode(
                            gamemode = Gamemode.valueOf(playmode),
                            role = Role.fromOsuId(osuGroup.id)
                        )
                    }
                } else {
                    // Unsupported usergroup
                    null
                }
            }?.flatten() ?: emptyList()
        )
    }

    fun findUserFromId(osuApiToken: String, osuId: String): User? {
        if (osuId == "0" || osuId == "-1" || osuId == "-2") return null

        val databaseUser = dataSource.findUser(osuId)

        if (databaseUser == null) {
            val osuUser = osuHttpClient.findUserWithId(osuApiToken, osuId)

            if (osuUser == null) {
                // Should only end up here if the user is restricted or the provided id is invalid
                return null
            }

            val newUser = convertOsuUserToUser(osuUser)

            dataSource.saveUser(newUser)
            return newUser
        } else {
            return databaseUser
        }
    }
}