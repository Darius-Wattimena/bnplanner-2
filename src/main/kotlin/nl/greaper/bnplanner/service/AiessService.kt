package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.*
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.aiess.AiessBeatmapEvent
import nl.greaper.bnplanner.model.aiess.AiessResponse
import nl.greaper.bnplanner.model.aiess.AiessUserEvent
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus.*
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import org.springframework.stereotype.Service

@Service
class AiessService(
    private val beatmapService: BeatmapService,
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService,
    private val discordClient: DiscordWebhookClient
) {
    fun processAiessBeatmapEvent(event: AiessBeatmapEvent): AiessResponse {
        val databaseBeatmap = beatmapService.findBeatmap(event.osuId)
            ?: return AiessResponse(false, "Could not find beatmap on bnplanner")

        if (event.status == Disqualified || event.status == Popped) {
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
        if (beatmap.status == newStatus) return
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
            Qualified -> NOMINATED_STATUS_ICON
            Bubbled -> BUBBLED_STATUS_ICON
            Disqualified -> DISQUALIFIED_STATUS_ICON
            Popped -> POPPED_STATUS_ICON
            Pending -> UPDATED_STATUS_ICON
            Ranked -> RANKED_STATUS_ICON
            Graved -> GRAVED_STATUS_ICON
            Unfinished -> UNFINISHED_STATUS_ICON
        }
    }

    fun processAiessUserEvent(event: AiessUserEvent): Boolean {
        return false
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
