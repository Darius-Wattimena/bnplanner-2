package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.client.DiscordClient
import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.UserDataSource
import nl.greaper.bnplanner.datasource.UserRecalculateDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.UserGamemode
import nl.greaper.bnplanner.model.UserRecalculate
import nl.greaper.bnplanner.model.aiess.AiessUserEvent
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.MeGroup
import nl.greaper.bnplanner.util.parseJwtToken
import org.springframework.stereotype.Service

@Service
class UserService(
    private val dataSource: UserDataSource,
    private val recalculateDataSource: UserRecalculateDataSource,
    private val osuHttpClient: OsuHttpClient,
    private val discordClient: DiscordClient
) {
    companion object {
        const val MAX_USERS = 20
        const val MISSING_USER_ID = "0"
    }

    private val log = KotlinLogging.logger { }

    fun searchUser(username: String?, gamemodes: Set<Gamemode>?, roles: Set<Role>?): List<User> {
        val searchResult = dataSource.searchUser(username, gamemodes ?: emptySet(), roles ?: emptySet())

        log.debug { "Found ${searchResult.size} users, taking max $MAX_USERS users." }

        return searchResult
            .sortedBy { it.username }
            .take(MAX_USERS)
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

    fun createUserByAiessEvent(event: AiessUserEvent): User {
        return User(event.userId, event.username, emptyList())
    }

    fun createTemporaryUser(osuId: String): User {
        val tempUser = User(osuId, osuId, emptyList())
        recalculateDataSource.insertOne(UserRecalculate(osuId))
        dataSource.saveUser(tempUser)
        return tempUser
    }

    /**
     * Find an osu user via the API and save them in the database for later use
     */
    fun forceFindUserById(osuApiToken: String, osuId: String, logEventToDiscord: Boolean = true): User? {
        val editor = getEditor(osuApiToken)
        val osuUser = osuHttpClient.findUserWithId(osuApiToken, osuId)

        if (osuUser == null) {
            // Should only end up here if the user is restricted or the provided id is invalid
            val restrictedUser = User(osuId, "RESTRICTED", emptyList(), restricted = true)
            dataSource.saveUser(restrictedUser)
            log.info { "[CREATE] ${editor?.username} added restricted user (osuId = $osuId)" }

            discordClient.send(
                description = "Could not find user with id $osuId, created restricted user",
                color = EmbedColor.ORANGE,
                thumbnail = EmbedThumbnail("https://a.ppy.sh/$osuId"),
                footer = EmbedFooter("Nomination Planner"),
                confidential = true,
                gamemodes = listOf()
            )

            return restrictedUser
        }

        val newUser = convertOsuUserToUser(osuUser)

        dataSource.saveUser(newUser)

        if (logEventToDiscord) {
            log.info { "[CREATE] ${editor?.username} added user ${newUser.username} (osuId = $osuId)" }
            discordClient.send(
                description = "Created user $osuId, with username: ${newUser.username}",
                color = EmbedColor.BLUE,
                thumbnail = EmbedThumbnail("https://a.ppy.sh/$osuId"),
                footer = EmbedFooter("Nomination Planner"),
                confidential = true,
                gamemodes = listOf()
            )
        }

        return newUser
    }

    fun findUserById(osuId: String): User? {
        if (osuId == "-1" || osuId == "-2") return null

        if (osuId == MISSING_USER_ID) {
            return User(
                MISSING_USER_ID,
                "None",
                gamemodes = emptyList()
            )
        }

        return dataSource.findUser(osuId)
    }

    fun getEditorId(osuApiToken: String): String? {
        val claims = parseJwtToken(osuApiToken) ?: return null

        return claims.subject
    }

    fun getEditor(osuApiToken: String): User? {
        val editorId = getEditorId(osuApiToken) ?: return null
        return findUserById(editorId)
    }

    fun deleteUser(user: User) {
        dataSource.deleteUser(user)
    }
}
