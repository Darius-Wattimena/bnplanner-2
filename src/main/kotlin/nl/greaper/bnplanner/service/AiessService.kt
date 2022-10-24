package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.CREATED_BEATMAP_ICON
import nl.greaper.bnplanner.DISQUALIFY_STATUS_ICON
import nl.greaper.bnplanner.GRAVED_STATUS_ICON
import nl.greaper.bnplanner.NOMINATE_STATUS_ICON
import nl.greaper.bnplanner.QUALIFY_STATUS_ICON
import nl.greaper.bnplanner.RANKED_STATUS_ICON
import nl.greaper.bnplanner.RESET_STATUS_ICON
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
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapNominator
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.osu.MeGroup
import nl.greaper.bnplanner.model.toReadableName
import nl.greaper.bnplanner.util.toReadableName
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AiessService(
    private val beatmapService: BeatmapService,
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService,
    private val userDataSource: UserDataSource,
    private val discordClient: DiscordWebhookClient
) {
    val log = KotlinLogging.logger { }

    private val instantStatus = listOf(BeatmapStatus.Ranked, BeatmapStatus.Graved, BeatmapStatus.Pending, BeatmapStatus.Unfinished)

    fun processAiessBeatmapGamemodeEvent(databaseBeatmap: Beatmap, status: BeatmapStatus, userId: String, username: String, gamemodes: List<Gamemode>) {
        val changingGamemodes = gamemodes.map { gamemode ->
            databaseBeatmap.gamemodes.find { it.gamemode == gamemode }
                ?: BeatmapGamemode(
                    gamemode = gamemode,
                    nominators = listOf(
                        BeatmapNominator("0", false),
                        BeatmapNominator("0", false)
                    ),
                    isReady = false
                )
        }

        val updatedBeatmap = beatmapService.updateBeatmapGamemodes(databaseBeatmap, changingGamemodes, status) { updatingGamemode ->
            val (currentFirstNominator, currentSecondNominator) = updatingGamemode.nominators.let { it[0] to it[1] }

            val newNominators = determineNominators(
                currentFirstNominator = currentFirstNominator,
                currentSecondNominator = currentSecondNominator,
                beatmapStatus = status,
                userId = userId,
                username = username,
                databaseBeatmap = databaseBeatmap,
                updatingGamemode = updatingGamemode
            )

            updatingGamemode.copy(nominators = newNominators)
        }

        beatmapDataSource.update(updatedBeatmap)
    }

    fun createFallbackBeatmap(event: AiessBeatmapEvent, userId: String?, username: String?): Beatmap {
        val addDate = Instant.now()

        val preparedGamemodes = event.modes.map { mode ->
            BeatmapGamemode(
                gamemode = mode,
                nominators = listOf(
                    BeatmapNominator("0", false),
                    BeatmapNominator("0", false)
                ),
                isReady = false
            )
        }

        val mapper = userService.findUserById(event.mapperId)

        // Mapper is unknown so we need to create one
        if (mapper == null) {
            val mapper = User(event.mapperId, event.mapper, event.modes.map { mode -> UserGamemode(mode, Role.Mapper) })

            userDataSource.saveUser(mapper)
        }

        return Beatmap(
            osuId = event.beatmapSetId,
            artist = event.artist,
            title = event.title,
            note = "",
            mapper = event.mapper,
            mapperId = event.mapperId,
            status = event.status,
            gamemodes = preparedGamemodes,
            dateAdded = addDate,
            dateUpdated = addDate,
            dateRanked = null
        ).also { logBeatmapAdded(it, event.status, userId, username) }
    }

    fun processAiessBeatmapEvent(event: AiessBeatmapEvent): AiessResponse {
        if (instantStatus.none { it == event.status }) {
            if (event.userId != null && event.username != null) {
                val databaseBeatmap = beatmapService.findBeatmap(event.beatmapSetId)
                    ?: createFallbackBeatmap(event, event.userId, event.username)

                if (event.status == BeatmapStatus.Disqualified || event.status == BeatmapStatus.Reset) {
                    val updatedGamemodes = databaseBeatmap.gamemodes.map { gamemode ->
                        gamemode.copy(nominators = gamemode.nominators.map { it.copy(hasNominated = false) })
                    }

                    logNomination(databaseBeatmap, event.status, event.userId, event.username)

                    beatmapDataSource.update(
                        databaseBeatmap.copy(
                            gamemodes = updatedGamemodes,
                            dateUpdated = Instant.now()
                        )
                    )
                    return AiessResponse(true)
                }

                if (event.modes.isNotEmpty()) {
                    processAiessBeatmapGamemodeEvent(databaseBeatmap, event.status, event.userId, event.username, event.modes)

                    return AiessResponse(true)
                }

                return AiessResponse(false, "No modes are provided")
            }

            return AiessResponse(false, "A userId or userName needs to be provided when the beatmap isn't ranked")
        } else {
            val databaseBeatmap = beatmapService.findBeatmap(event.beatmapSetId)
                ?: createFallbackBeatmap(event, null, null)

            val now = Instant.now()

            beatmapDataSource.update(
                databaseBeatmap.copy(
                    status = event.status,
                    dateUpdated = now,
                    dateRanked = now
                )
            )
            logInstantStatus(databaseBeatmap, event.status)

            return AiessResponse(true)
        }
    }

    private fun determineNominators(
        currentFirstNominator: BeatmapNominator,
        currentSecondNominator: BeatmapNominator,
        beatmapStatus: BeatmapStatus,
        userId: String,
        username: String,
        databaseBeatmap: Beatmap,
        updatingGamemode: BeatmapGamemode
    ): List<BeatmapNominator> {
        val newNominators = when {
            currentFirstNominator.nominatorId == userId -> {
                val newNominator = currentFirstNominator.copy(hasNominated = true)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                newNominator to currentSecondNominator
            }
            currentSecondNominator.nominatorId == userId -> {
                val newNominator = currentSecondNominator.copy(hasNominated = true)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                currentFirstNominator to newNominator
            }
            currentFirstNominator.nominatorId == "0" -> {
                val newNominator = BeatmapNominator(
                    nominatorId = userId,
                    hasNominated = true
                )

                beatmapService.logAiessUpdatedNominator(databaseBeatmap, currentFirstNominator.nominatorId, userId, updatingGamemode.gamemode)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                newNominator to currentSecondNominator
            }
            currentSecondNominator.nominatorId == "0" -> {
                val newNominator = BeatmapNominator(
                    nominatorId = userId,
                    hasNominated = true
                )

                beatmapService.logAiessUpdatedNominator(databaseBeatmap, currentSecondNominator.nominatorId, userId, updatingGamemode.gamemode)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                currentFirstNominator to newNominator
            }
            !currentFirstNominator.hasNominated -> {
                val newNominator = BeatmapNominator(
                    nominatorId = userId,
                    hasNominated = true
                )

                beatmapService.logAiessUpdatedNominator(databaseBeatmap, currentFirstNominator.nominatorId, userId, updatingGamemode.gamemode)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                newNominator to currentSecondNominator
            }
            !currentSecondNominator.hasNominated -> {
                val newNominator = BeatmapNominator(
                    nominatorId = userId,
                    hasNominated = true
                )

                beatmapService.logAiessUpdatedNominator(databaseBeatmap, currentSecondNominator.nominatorId, userId, updatingGamemode.gamemode)
                logNomination(databaseBeatmap, beatmapStatus, userId, username, updatingGamemode.gamemode)
                currentFirstNominator to newNominator
            }
            else -> {
                log.error { "Two nominators already nominated this beatmap for this gamemode, beatmap is out of sync with Aiess" }
                currentFirstNominator to currentSecondNominator
            }
        }

        return newNominators.toList()
    }

    private fun logInstantStatus(beatmap: Beatmap, newStatus: BeatmapStatus) {
        val messageIcon = getMessageIcon(newStatus)

        discordClient.send(
            """**$messageIcon Updated status to $newStatus**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by ${beatmap.mapper}
            """.prependIndent(),
            getMessageColor(newStatus),
            EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            EmbedFooter("Aiess"),
            confidential = true
        )
    }

    private fun logBeatmapAdded(beatmap: Beatmap, newStatus: BeatmapStatus, nominatorId: String?, username: String?) {
        val footer = if (nominatorId != null && username != null) {
            EmbedFooter(username, "https://a.ppy.sh/$nominatorId")
        } else {
            EmbedFooter("Aiess")
        }

        discordClient.send(
            """$CREATED_BEATMAP_ICON **Created**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}})
            """.prependIndent(),
            getMessageColor(newStatus),
            EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            footer,
            confidential = true
        )
    }

    private fun logNomination(beatmap: Beatmap, newStatus: BeatmapStatus, nominatorId: String, username: String, gamemode: Gamemode? = null) {
        val messageIcon = getMessageIcon(newStatus)
        val gamemodeText = gamemode?.let { gamemode.toReadableName() } ?: ""

        discordClient.send(
            """**$messageIcon Updated status to $newStatus**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}}) [$gamemodeText]
            """.prependIndent(),
            getMessageColor(newStatus),
            EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            EmbedFooter(username, "https://a.ppy.sh/$nominatorId"),
            confidential = true
        )
    }

    private fun getMessageIcon(status: BeatmapStatus): String {
        return when (status) {
            BeatmapStatus.Qualified -> QUALIFY_STATUS_ICON
            BeatmapStatus.Nominated -> NOMINATE_STATUS_ICON
            BeatmapStatus.Disqualified -> DISQUALIFY_STATUS_ICON
            BeatmapStatus.Reset -> RESET_STATUS_ICON
            BeatmapStatus.Pending -> UPDATED_STATUS_ICON
            BeatmapStatus.Ranked -> RANKED_STATUS_ICON
            BeatmapStatus.Graved -> GRAVED_STATUS_ICON
            BeatmapStatus.Unfinished -> UNFINISHED_STATUS_ICON
        }
    }

    private fun getMessageColor(status: BeatmapStatus): EmbedColor {
        return when (status) {
            BeatmapStatus.Qualified,
            BeatmapStatus.Nominated,
            BeatmapStatus.Pending -> EmbedColor.BLUE
            BeatmapStatus.Disqualified,
            BeatmapStatus.Reset -> EmbedColor.ORANGE
            BeatmapStatus.Ranked -> EmbedColor.GREEN
            BeatmapStatus.Graved -> EmbedColor.GREY
            BeatmapStatus.Unfinished -> EmbedColor.PURPLE
        }
    }

    fun processAiessUserEvent(event: AiessUserEvent): AiessResponse {
        val changingUser = userService.findUserById(event.userId)
            ?: userService.createUserByAiessEvent(event)
        val userRole = Role.fromOsuId(event.groupId)
        val oldGamemode = changingUser.gamemodes.find {
            it.gamemode == event.gamemode && it.role == userRole
        }

        when (event.type) {
            AiessUserEventType.add -> {
                if (MeGroup.SupportedGroups.contains(event.groupId)) {

                    // Check if the usergroup needs to be replaced or if we can just add a new one
                    val updateUser = if (oldGamemode != null) {
                        // User most likely moved to a different role in the gamemode, update it
                        val updatedGamemode = oldGamemode.copy(role = userRole)
                        val untouchedGamemodes = changingUser.gamemodes.filter { it.gamemode != event.gamemode }

                        changingUser.copy(gamemodes = untouchedGamemodes + updatedGamemode)
                            .also { logUserMove(it, event, oldGamemode, updatedGamemode) }
                    } else {
                        // User is not part of this gamemode, add them
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
                // User has been removed from a gamemode we don't know anything about
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
            "Moved [${user.username}](https://osu.ppy.sh/users/${user.osuId})\nfrom ${oldGamemode.role.toReadableName()} to ${updatedGamemode.role.toReadableName()}"
        } else if (updatedGamemode != null) {
            "Added [${user.username}](https://osu.ppy.sh/users/${user.osuId})\nto ${updatedGamemode.role.toReadableName()}"
        } else if (oldGamemode != null) {
            "Removed [${user.username}](https://osu.ppy.sh/users/${user.osuId})\nfrom ${oldGamemode.role.toReadableName()}"
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
            EmbedFooter("Aiess"),
            confidential = true
        )
    }
}
