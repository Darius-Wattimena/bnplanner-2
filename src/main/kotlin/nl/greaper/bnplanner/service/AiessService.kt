package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.*
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.aiess.AiessBeatmapEvent
import nl.greaper.bnplanner.model.aiess.AiessUserEvent
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapNominator
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
    fun processAiessBeatmapEvent(event: AiessBeatmapEvent): Boolean {
        val databaseBeatmap = beatmapService.findBeatmap(event.osuId) ?: return false

        val changingGamemode = databaseBeatmap.gamemodes.find { gamemode ->
            gamemode.nominators.any { it.nominatorId == event.nominatorOsuId }
        } ?: return false

        if (event.status == Disqualified || event.status == Popped) {
            val updatedGamemodes = databaseBeatmap.gamemodes.map { gamemode ->
                gamemode.copy(nominators = gamemode.nominators.map { it.copy(hasNominated = false) }.toSet())
            }.toSet()

            logNomination(databaseBeatmap, event.status)

            beatmapDataSource.update(databaseBeatmap.copy(gamemodes = updatedGamemodes))
            return true
        }

        val updatedBeatmap = beatmapService.updateBeatmapGamemode(databaseBeatmap, changingGamemode.gamemode) { updatingGamemode ->
            val (currentFirstNominator, currentSecondNominator) = updatingGamemode.nominators.toList().let {
                it.getOrNull(0) to it.getOrNull(1)
            }

            val newNominators = when {
                currentFirstNominator?.nominatorId == event.nominatorOsuId -> {
                    val newNominator = currentFirstNominator.copy(hasNominated = true)
                    logNomination(databaseBeatmap, event.status, updatingGamemode.gamemode)
                    newNominator to currentSecondNominator
                }
                currentSecondNominator?.nominatorId == event.nominatorOsuId -> {
                    val newNominator = currentSecondNominator.copy(hasNominated = true)
                    logNomination(databaseBeatmap, event.status, updatingGamemode.gamemode)
                    currentFirstNominator to newNominator
                }
                else -> {
                    // TODO neither of the planned BNs on the set nominated the beatmap
                    currentFirstNominator to currentSecondNominator
                }
            }

            val updatedGamemode = updatingGamemode.copy(
                nominators = setOfNotNull(newNominators.first, newNominators.second)
            )

            updatedGamemode
        } ?: return false

        beatmapDataSource.update(updatedBeatmap.copy(status = event.status))

        return true
    }

    private fun logNomination(beatmap: Beatmap, newStatus: BeatmapStatus, gamemode: Gamemode? = null) {
        if (beatmap.status == newStatus) return
        val messageIcon = getMessageIcon(newStatus)

        discordClient.send(
            """**$messageIcon Updated status to $newStatus ${gamemode?.let { "[${it.toReadable()}]" }}**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by ${beatmap.mapper}
            """.prependIndent(),
            EmbedColor.BLUE,
            EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            EmbedFooter("Aiess"),
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
