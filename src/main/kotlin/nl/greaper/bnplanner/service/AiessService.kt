package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.BUBBLED_STATUS_ICON
import nl.greaper.bnplanner.DISQUALIFIED_STATUS_ICON
import nl.greaper.bnplanner.GRAVED_STATUS_ICON
import nl.greaper.bnplanner.NOMINATED_STATUS_ICON
import nl.greaper.bnplanner.POPPED_STATUS_ICON
import nl.greaper.bnplanner.RANKED_STATUS_ICON
import nl.greaper.bnplanner.UNFINISHED_STATUS_ICON
import nl.greaper.bnplanner.UPDATED_STATUS_ICON
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.datasource.UserDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.UserGamemode
import nl.greaper.bnplanner.model.aiess.AiessBeatmapEvent
import nl.greaper.bnplanner.model.aiess.AiessResponse
import nl.greaper.bnplanner.model.aiess.AiessUserEvent
import nl.greaper.bnplanner.model.aiess.AiessUserEventType
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.osu.MeGroup
import org.springframework.stereotype.Service

@Service
class AiessService(
    private val beatmapService: BeatmapService,
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService,
    private val userDataSource: UserDataSource,
    private val discordClient: DiscordWebhookClient
) {
    fun processAiessBeatmapEvent(event: AiessBeatmapEvent): AiessResponse {
        val databaseBeatmap = beatmapService.findBeatmap(event.osuId)
            ?: return AiessResponse(false, "Could not find beatmap on bnplanner")

        if (event.status == BeatmapStatus.Disqualified || event.status == BeatmapStatus.Popped) {
            val updatedGamemodes = databaseBeatmap.gamemodes.map { gamemode ->
                gamemode.copy(nominators = gamemode.nominators.map { it.copy(hasNominated = false) })
            }

            logNomination(databaseBeatmap, event.status, event.nominatorId)

            beatmapDataSource.update(databaseBeatmap.copy(gamemodes = updatedGamemodes))
            return AiessResponse(true)
        }

        val changingGamemode = databaseBeatmap.gamemodes.find { gamemode ->
            gamemode.nominators.any { it.nominatorId == event.nominatorId }
        } ?: return AiessResponse(false, "Nominator isn't part of this mapset")

        val updatedBeatmap = beatmapService.updateBeatmapGamemode(databaseBeatmap, changingGamemode.gamemode) { updatingGamemode ->
            val (currentFirstNominator, currentSecondNominator) = updatingGamemode.nominators.toList().let {
                it[0] to it[1]
            }

            val newNominators = when {
                currentFirstNominator.nominatorId == event.nominatorId -> {
                    val newNominator = currentFirstNominator.copy(hasNominated = true)
                    logNomination(databaseBeatmap, event.status, event.nominatorId, updatingGamemode.gamemode)
                    newNominator to currentSecondNominator
                }
                currentSecondNominator.nominatorId == event.nominatorId -> {
                    val newNominator = currentSecondNominator.copy(hasNominated = true)
                    logNomination(databaseBeatmap, event.status, event.nominatorId, updatingGamemode.gamemode)
                    currentFirstNominator to newNominator
                }
                else -> {
                    // TODO neither of the planned BNs on the set nominated the beatmap
                    currentFirstNominator to currentSecondNominator
                }
            }

            val updatedGamemode = updatingGamemode.copy(
                nominators = listOf(newNominators.first, newNominators.second)
            )

            updatedGamemode
        } ?: return AiessResponse(false, "Mapset gamemode isn't registered on the BN planner")

        beatmapDataSource.update(updatedBeatmap.copy(status = event.status))

        return AiessResponse(true)
    }

    private fun logNomination(beatmap: Beatmap, newStatus: BeatmapStatus, nominatorId: String, gamemode: Gamemode? = null) {
        val messageIcon = getMessageIcon(newStatus)
        val gamemodeText = gamemode?.let { "[${it.toReadable()}]" } ?: ""

        discordClient.send(
            """**$messageIcon Updated status to $newStatus $gamemodeText**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by ${beatmap.mapper}
            """.prependIndent(),
            EmbedColor.BLUE,
            EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            EmbedFooter("Aiess", "https://a.ppy.sh/$nominatorId"),
            confidential = true
        )
    }

    private fun getMessageIcon(status: BeatmapStatus): String {
        return when (status) {
            BeatmapStatus.Qualified -> NOMINATED_STATUS_ICON
            BeatmapStatus.Bubbled -> BUBBLED_STATUS_ICON
            BeatmapStatus.Disqualified -> DISQUALIFIED_STATUS_ICON
            BeatmapStatus.Popped -> POPPED_STATUS_ICON
            BeatmapStatus.Pending -> UPDATED_STATUS_ICON
            BeatmapStatus.Ranked -> RANKED_STATUS_ICON
            BeatmapStatus.Graved -> GRAVED_STATUS_ICON
            BeatmapStatus.Unfinished -> UNFINISHED_STATUS_ICON
        }
    }

    fun processAiessUserEvent(event: AiessUserEvent): AiessResponse {
        val changingUser = userService.findUserById(event.userId)
            ?: userService.createTemporaryUser(event.userId)
        val oldGamemode = changingUser.gamemodes.find { it.gamemode == event.gamemode }

        when (event.type) {
            AiessUserEventType.add -> {
                if (MeGroup.SupportedGroups.contains(event.groupId)) {
                    val userRole = Role.fromOsuId(event.groupId)

                    // Check if the usergroup needs to be replaced or if we can just add a new one
                    val updateUser = if (oldGamemode != null) {
                        val updatedGamemode = oldGamemode.copy(role = userRole)
                        val untouchedGamemodes = changingUser.gamemodes.filter { it.gamemode != event.gamemode }

                        changingUser.copy(gamemodes = untouchedGamemodes + updatedGamemode)
                            .also { logUserMove(it, event, oldGamemode, updatedGamemode) }
                    } else {
                        val newGamemode = UserGamemode(event.gamemode, userRole)
                        changingUser.copy(gamemodes = changingUser.gamemodes + newGamemode)
                            .also { logUserMove(it, event, null, newGamemode) }
                    }

                    userDataSource.saveUser(updateUser)
                }
            }
            AiessUserEventType.remove -> {
                if (oldGamemode != null) {
                    val updateUser = changingUser.copy(gamemodes = changingUser.gamemodes.filter { it.gamemode != event.gamemode })

                    userDataSource.saveUser(updateUser)

                    logUserMove(updateUser, event, oldGamemode, null)
                }
            }
        }

        return AiessResponse(true)
    }

    fun logUserMove(user: User, event: AiessUserEvent, oldGamemode: UserGamemode?, updatedGamemode: UserGamemode?) {
        val color = if (event.type == AiessUserEventType.add) {
            EmbedColor.GREEN
        } else {
            EmbedColor.RED
        }

        val topLine = if (oldGamemode != null && updatedGamemode != null) {
            "Moved [${user.username}](https://osu.ppy.sh/users/${user.osuId}) from ${oldGamemode.role} to ${updatedGamemode.role}"
        } else if (updatedGamemode != null) {
            "Added [${user.username}](https://osu.ppy.sh/users/${user.osuId}) to ${updatedGamemode.role}"
        } else if (oldGamemode != null) {
            "Removed [${user.username}](https://osu.ppy.sh/users/${user.osuId}) from ${oldGamemode.role}"
        } else {
            "ERROR? User moved happened but no clue what"
        }

        val text = if (event.type == AiessUserEventType.add) {
            """**$topLine**
                Gamemode ${event.gamemode}
            """.prependIndent()
        } else {
            """**$topLine**
                Gamemode ${event.gamemode}
            """.prependIndent()
        }

        discordClient.send(
            description = text,
            color = color,
            thumbnail = EmbedThumbnail("https://a.ppy.sh/${user.osuId}"),
            EmbedFooter("Aiess", "https://a.ppy.sh/${user.osuId}"),
            confidential = true
        )
    }
}

private fun Gamemode.toReadable(): String {
    return when (this) {
        Gamemode.osu -> "osu"
        Gamemode.taiko -> "taiko"
        Gamemode.fruits -> "catch"
        Gamemode.mania -> "mania"
    }
}
