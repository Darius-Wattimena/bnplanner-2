package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.UserDataSource
import nl.greaper.bnplanner.datasource.UserRecalculateDataSource
import nl.greaper.bnplanner.model.*
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.MeGroup
import org.springframework.stereotype.Service

@Service
class UserService(
    private val dataSource: UserDataSource,
    private val recalculateDataSource: UserRecalculateDataSource,
    private val osuHttpClient: OsuHttpClient,
    private val discordClient: DiscordWebhookClient
) {
    companion object {
        const val MAX_USERS = 100
    }

    val log = KotlinLogging.logger { }

    fun searchUser(username: String?, gamemodes: Set<Gamemode>?, roles: Set<Role>?): List<User> {
        val searchResult = dataSource.searchUser(username, gamemodes ?: emptySet(), roles ?: emptySet())

        log.info { "Found ${searchResult.size} users, taking max $MAX_USERS users." }

        return searchResult.take(MAX_USERS)
    }

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

    fun createTemporaryUser(osuId: String): User {
        val tempUser = User(osuId, osuId, emptyList())
        recalculateDataSource.insertOne(UserRecalculate(osuId))
        dataSource.saveUser(tempUser)
        return tempUser
    }

    fun forceFindUserFromId(osuApiToken: String, osuId: String): User? {
        val osuUser = osuHttpClient.findUserWithId(osuApiToken, osuId)

        if (osuUser == null) {
            // Should only end up here if the user is restricted or the provided id is invalid
            val restrictedUser = User(osuId, "RESTRICTED", emptyList(), restricted = true)
            dataSource.saveUser(restrictedUser)

            discordClient.send(
                    "Could not find user with id $osuId, created restricted user",
                    EmbedColor.ORANGE,
                    EmbedThumbnail("https://a.ppy.sh/$osuId"),
                    EmbedFooter("Nomination Planner"),
            )

            return restrictedUser
        }

        val newUser = convertOsuUserToUser(osuUser)

        dataSource.saveUser(newUser)

        discordClient.send(
                "Created user $osuId, with username: ${newUser.username}",
                EmbedColor.BLUE,
                EmbedThumbnail("https://a.ppy.sh/$osuId"),
                EmbedFooter("Nomination Planner"),
        )

        return newUser
    }

    fun findUserFromId(osuApiToken: String, osuId: String): User? {
        if (osuId == "0" || osuId == "-1" || osuId == "-2") return null

        return dataSource.findUser(osuId) ?: createTemporaryUser(osuId)
    }
}
