package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.ADDED_NOMINATOR_ICON
import nl.greaper.bnplanner.CHANGE_BEATMAP_NOTE_ICON
import nl.greaper.bnplanner.CREATED_BEATMAP_ICON
import nl.greaper.bnplanner.DELETED_BEATMAP_ICON
import nl.greaper.bnplanner.REMOVED_NOMINATOR_ICON
import nl.greaper.bnplanner.client.DiscordClient
import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.PageLimit
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapNominator
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus.Companion.toPriorityStatus
import nl.greaper.bnplanner.model.beatmap.ExposedBeatmap
import nl.greaper.bnplanner.model.beatmap.ExposedBeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.ExposedBeatmapNominator
import nl.greaper.bnplanner.model.beatmap.LegacyBeatmap
import nl.greaper.bnplanner.model.beatmap.NewBeatmap
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.service.UserService.Companion.MISSING_USER_ID
import nl.greaper.bnplanner.util.getEmojiIcon
import nl.greaper.bnplanner.util.quote
import nl.greaper.bnplanner.util.toReadableName
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.bson
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.`in`
import org.litote.kmongo.or
import org.litote.kmongo.regex
import org.springframework.stereotype.Service
import java.lang.StringBuilder
import java.time.Instant

@Service
class BeatmapService(
    private val dataSource: BeatmapDataSource,
    private val userService: UserService,
    private val osuHttpClient: OsuHttpClient,
    private val discordClient: DiscordClient
) {
    val log = KotlinLogging.logger { }

    fun findBeatmap(id: String): Beatmap? {
        return dataSource.findById(id)
    }

    fun findExposedBeatmap(osuApiToken: String, id: String): ExposedBeatmap? {
        return dataSource.findById(id)?.toExposedBeatmap()
    }

    fun deleteBeatmap(osuApiToken: String, osuId: String) {
        val databaseBeatmap = dataSource.findById(osuId) ?: return
        dataSource.deleteById(osuId).also {
            if (it.deletedCount > 0) {
                logBeatmapDelete(osuApiToken, databaseBeatmap)
            }
        }
    }

    fun countBeatmaps(
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage,
        gamemodes: Set<Gamemode>,
        missingNominator: Set<Gamemode>
    ): Int {
        return dataSource.count(setupFilter(artist, title, mapper, status, nominators, page, gamemodes, missingNominator))
    }

    fun findBeatmaps(
        osuApiToken: String,
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage,
        from: Int,
        to: Int,
        gamemodes: Set<Gamemode>,
        missingNominator: Set<Gamemode>
    ): List<ExposedBeatmap> {
        return dataSource.findAll(
            setupFilter(artist, title, mapper, status, nominators, page, gamemodes, missingNominator),
            from,
            to
        ).mapNotNull { it.toExposedBeatmap() }
    }

    fun findBeatmaps(
        osuApiToken: String,
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage,
        pageNumber: Int,
        pageLimit: PageLimit,
        gamemodes: Set<Gamemode>,
        missingNominator: Set<Gamemode>
    ): List<ExposedBeatmap> {
        return dataSource.findAll(
            setupFilter(artist, title, mapper, status, nominators, page, gamemodes, missingNominator),
            pageNumber,
            pageLimit
        ).mapNotNull { it.toExposedBeatmap() }
    }

    fun addBeatmap(osuApiToken: String, input: NewBeatmap): ExposedBeatmap? {
        val addDate = Instant.now()

        val databaseBeatmap = dataSource.findById(input.osuId)

        // Only request a beatmap from the api if we don't have it
        if (databaseBeatmap != null) {
            return databaseBeatmap.toExposedBeatmap()
        }

        val parsedToken = osuApiToken.removePrefix("Bearer ")
        val osuBeatmap = osuHttpClient.findBeatmapWithId(parsedToken, input.osuId) ?: return null

        val preparedBeatmapGamemodes = input.gamemodes.map {
            BeatmapGamemode(
                gamemode = it,
                nominators = listOf(
                    BeatmapNominator(MISSING_USER_ID, false),
                    BeatmapNominator(MISSING_USER_ID, false)
                ),
                isReady = false
            )
        }

        val databaseUser = userService.findUserById(osuBeatmap.user_id)

        if (databaseUser == null || databaseUser.restricted == true) {
            // Sleep the thread if we need to find the user as well to avoid the call being blocked
            Thread.sleep(1_000)
            userService.forceFindUserById(parsedToken, osuBeatmap.user_id)
        }

        val newBeatmap = Beatmap(
            osuId = input.osuId,
            artist = osuBeatmap.artist,
            title = osuBeatmap.title,
            note = "",
            mapper = osuBeatmap.creator,
            mapperId = osuBeatmap.user_id,
            status = BeatmapStatus.Pending,
            gamemodes = preparedBeatmapGamemodes,
            dateAdded = addDate,
            dateUpdated = addDate,
            dateRanked = null
        )

        dataSource.insertOne(newBeatmap).also { logBeatmapAdded(parsedToken, newBeatmap) }

        return newBeatmap.toExposedBeatmap()
    }

    fun syncBeatmap(osuApiToken: String, beatmap: Beatmap) {
        // Don't sync a beatmap if it is already marked as ranked
        if (beatmap.status == BeatmapStatus.Ranked) {
            return
        }

        val parsedToken = osuApiToken.removePrefix("Bearer ")
        val osuBeatmap = osuHttpClient.findBeatmapWithId(parsedToken, beatmap.osuId)

        if (osuBeatmap == null) {
            log.warn { "Could not find beatmap when requesting osu api (osuId: ${beatmap.osuId})" }
            return
        }

        val newStatus = when (osuBeatmap.ranked) {
            1, 2 -> BeatmapStatus.Ranked
            3 -> BeatmapStatus.Qualified
            else -> null
        }

        val dateRanked = if (newStatus == BeatmapStatus.Ranked) {
            osuBeatmap.ranked_date ?: Instant.now()
        } else {
            null
        }

        val updatedBeatmap = beatmap.copy(
            artist = osuBeatmap.artist,
            title = osuBeatmap.title,
            mapper = osuBeatmap.creator,
            status = newStatus ?: beatmap.status,
            dateUpdated = osuBeatmap.last_updated,
            dateRanked = dateRanked
        )

        dataSource.update(updatedBeatmap)
    }

    private fun setupFilter(
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage,
        gamemodes: Set<Gamemode>,
        missingNominator: Set<Gamemode>
    ): Bson {
        val filters = mutableListOf<Bson>()

        artist?.let { filters += Beatmap::artist regex quote(it).toRegex(RegexOption.IGNORE_CASE) }
        title?.let { filters += Beatmap::title regex quote(it).toRegex(RegexOption.IGNORE_CASE) }
        mapper?.let { filters += Beatmap::mapper regex quote(it).toRegex(RegexOption.IGNORE_CASE) }

        if (nominators.isNotEmpty()) {
            filters += Beatmap::gamemodes / BeatmapGamemode::nominators / BeatmapNominator::nominatorId `in` nominators
        }

        if (gamemodes.isNotEmpty()) {
            filters += Beatmap::gamemodes / BeatmapGamemode::gamemode `in` gamemodes
        }

        if (missingNominator.isNotEmpty()) {
            val missingNominatorFilters = missingNominator.map { gamemode ->
                and(
                    Beatmap::gamemodes / BeatmapGamemode::gamemode eq gamemode,
                    Beatmap::gamemodes / BeatmapGamemode::nominators / BeatmapNominator::nominatorId eq MISSING_USER_ID
                )
            }

            filters += or(missingNominatorFilters)
        }

        val parsedStatus = status.mapNotNull {
            when (page) {
                BeatmapPage.PENDING -> {
                    if (it == BeatmapStatus.Ranked || it == BeatmapStatus.Graved) {
                        null
                    } else it
                }
                BeatmapPage.RANKED -> {
                    if (it != BeatmapStatus.Ranked) {
                        null
                    } else it
                }
                BeatmapPage.GRAVEYARD -> {
                    if (it != BeatmapStatus.Graved) {
                        null
                    } else it
                }
            }
        }

        filters += if (parsedStatus.isNotEmpty()) {
            or(
                status.map {
                    "{ status : ${it.toPriorityStatus()} }  ".bson
                }
            )
        } else {
            when (page) {
                BeatmapPage.PENDING -> and(
                    "{ status : { \$ne : ${BeatmapStatus.Ranked.toPriorityStatus()} } }".bson,
                    "{ status : { \$ne : ${BeatmapStatus.Graved.toPriorityStatus()} } }".bson,
                )
                BeatmapPage.RANKED -> "{ status : ${BeatmapStatus.Ranked.toPriorityStatus()} }".bson
                BeatmapPage.GRAVEYARD -> "{ status : ${BeatmapStatus.Graved.toPriorityStatus()} }".bson
            }
        }

        return and(filters)
    }

    @Deprecated("Endpoint has been removed, to be removed")
    fun importLegacyBeatmaps(legacyBeatmaps: List<LegacyBeatmap>) {
        val convertedBeatmaps = legacyBeatmaps.mapNotNull { convertLegacyBeatmapToBeatmap(it) }
        dataSource.insertMany(convertedBeatmaps)
    }

    fun updateBeatmapNote(osuApiToken: String, osuId: String, newNote: String): Boolean {
        val databaseBeatmap = findBeatmap(osuId) ?: return false

        if (databaseBeatmap.note == newNote) {
            // Nothing to update
            return true
        }

        val updatedBeatmap = databaseBeatmap.copy(
            note = newNote,
            dateUpdated = Instant.now()
        )

        dataSource.update(updatedBeatmap)
        logBeatmapNoteChange(osuApiToken, updatedBeatmap)

        return true
    }

    fun updateBeatmapStatus(osuApiToken: String, osuId: String, newStatus: BeatmapStatus): Boolean {
        val databaseBeatmap = findBeatmap(osuId) ?: return false

        if (databaseBeatmap.status == newStatus) {
            // Nothing to update
            return true
        }

        val updatedBeatmap = databaseBeatmap.copy(
            status = newStatus,
            dateUpdated = Instant.now()
        )

        dataSource.update(updatedBeatmap)
        logBeatmapStatusChange(osuApiToken, updatedBeatmap)

        return true
    }

    fun updateBeatmapNominators(osuApiToken: String, beatmapId: String, beatmapGamemodes: List<BeatmapGamemode>): ExposedBeatmap? {
        val databaseBeatmap = findBeatmap(beatmapId) ?: return null

        val updatedBeatmap = databaseBeatmap.copy(
            gamemodes = beatmapGamemodes,
            dateUpdated = Instant.now()
        )

        dataSource.update(updatedBeatmap)
        logUpdatedNominators(osuApiToken, updatedBeatmap, databaseBeatmap)

        return updatedBeatmap.toExposedBeatmap()
    }

    fun updateBeatmapNominator(osuApiToken: String, osuId: String, gamemode: Gamemode, oldNominator: String, newNominator: String): ExposedBeatmap? {
        val databaseBeatmap = findBeatmap(osuId) ?: return null

        val updatedBeatmap = updateBeatmapGamemode(databaseBeatmap, gamemode) { updatingGamemode ->
            val (currentFirstNominator, currentSecondNominator) = updatingGamemode.nominators.let {
                it[0] to it[1]
            }

            val (newFirstNominator, newSecondNominator) = if (currentFirstNominator.nominatorId == oldNominator) {
                BeatmapNominator(newNominator, false) to currentSecondNominator
            } else {
                currentFirstNominator to BeatmapNominator(newNominator, false)
            }

            // Update the BeatmapGamemode to null when both nominators are missing and this is not the last gamemode of the beatmap set
            if (newFirstNominator.nominatorId == MISSING_USER_ID &&
                newSecondNominator.nominatorId == MISSING_USER_ID &&
                databaseBeatmap.gamemodes.size > 1
            ) {
                null
            } else {
                val updatedGamemode = updatingGamemode.copy(
                    nominators = listOf(newFirstNominator, newSecondNominator)
                )

                updatedGamemode
            }
        } ?: return null

        // Use the databaseBeatmap here, so we always log to all gamemodes even the deleted ones
        val gamemodesBeforeUpdate = databaseBeatmap.gamemodes.map { it.gamemode }
        dataSource.update(updatedBeatmap)
        logUpdatedNominators(osuApiToken, updatedBeatmap, oldNominator, newNominator, gamemode, gamemodesBeforeUpdate)

        return updatedBeatmap.toExposedBeatmap()
    }

    fun updateBeatmapGamemode(beatmap: Beatmap, gamemode: Gamemode, new: (old: BeatmapGamemode) -> BeatmapGamemode?): Beatmap? {
        val updatingGamemode = beatmap.gamemodes.find { it.gamemode == gamemode }

        if (updatingGamemode == null) {
            val newBeatmapGamemode = BeatmapGamemode(
                gamemode = gamemode,
                nominators = listOf(
                    BeatmapNominator(MISSING_USER_ID, false),
                    BeatmapNominator(MISSING_USER_ID, false)
                ),
                isReady = false
            )
            return updateBeatmapGamemode(beatmap, newBeatmapGamemode, new)
        }

        return updateBeatmapGamemode(beatmap, updatingGamemode, new)
    }

    fun updateBeatmapGamemodes(beatmap: Beatmap, updatingGamemodes: List<BeatmapGamemode>, status: BeatmapStatus, new: (old: BeatmapGamemode) -> BeatmapGamemode): Beatmap {
        var updateBeatmap = beatmap
        updatingGamemodes.forEach {
            updateBeatmap = updateBeatmapGamemode(updateBeatmap, it, new)
        }

        return updateBeatmap.copy(
            status = status
        )
    }

    fun updateBeatmapGamemode(beatmap: Beatmap, updatingGamemode: BeatmapGamemode, new: (old: BeatmapGamemode) -> BeatmapGamemode?): Beatmap {
        val newGamemode = new(updatingGamemode)

        // Only take the updated gamemode if the new gamemode isn't null
        val updatedGamemodes = if (newGamemode == null) {
            beatmap.gamemodes - updatingGamemode
        } else {
            beatmap.gamemodes - updatingGamemode + newGamemode
        }

        return beatmap.copy(
            gamemodes = updatedGamemodes,
            dateUpdated = Instant.now()
        )
    }

    fun logBeatmapAdded(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        log.info { "[CREATE] ${editor?.username} added (beatmap = ${beatmap.osuId})" }
        val gamemodes = beatmap.gamemodes.joinToString { it.gamemode.toReadableName() }

        val message = StringBuilder()
            .append("$CREATED_BEATMAP_ICON **Created**")
            .append("\nGamemodes: $gamemodes")
            .append("\n**[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId})")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.GREEN,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapDelete(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        log.info { "[DELETE] ${editor?.username} deleted (beatmap = ${beatmap.osuId})" }

        val message = StringBuilder()
            .append("$DELETED_BEATMAP_ICON **Deleted")
            .append("\n[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId})")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.RED,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapNoteChange(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        log.info { "[UPDATE] ${editor?.username} changed note (beatmap = ${beatmap.osuId})" }

        val message = StringBuilder()
            .append("$CHANGE_BEATMAP_NOTE_ICON **Updated note**")
            .append("\n```${beatmap.note.replace("""\n  +""".toRegex(), "\n")}```")
            .append("\n**[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId})")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.ORANGE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapStatusChange(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        log.info { "[UPDATE] ${editor?.username} changed status to ${beatmap.status.name} (beatmap = ${beatmap.osuId})" }

        val message = StringBuilder()
            .append("${beatmap.status.getEmojiIcon()} **Updated status to ${beatmap.status.name}")
            .append("\n[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId})")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.ORANGE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    private fun getChangedNominatorText(
        newNominator: User?,
        oldNominator: User?,
        isMultipleGamemodes: Boolean? = null,
        gamemode: Gamemode? = null
    ): String {
        val nominatorChangesText = StringBuilder("")

        // We don't have to create any changed nominator text when nothing changed in the first place
        if (newNominator?.osuId == oldNominator?.osuId) {
            return nominatorChangesText.toString()
        }

        if (newNominator != null && newNominator.osuId != MISSING_USER_ID) {
            nominatorChangesText.append("$ADDED_NOMINATOR_ICON **Added [${newNominator.username}](https://osu.ppy.sh/users/${newNominator.osuId})**")

            if (isMultipleGamemodes == true && gamemode != null) {
                nominatorChangesText.append(" [${gamemode.toReadableName()}]")
            }
        }

        if (oldNominator != null && oldNominator.osuId != MISSING_USER_ID) {
            // Add a \n if we added a nominator as well
            if (nominatorChangesText.toString() != "") {
                nominatorChangesText.append("\n")
            }

            nominatorChangesText.append("$REMOVED_NOMINATOR_ICON **Removed [${oldNominator.username}](https://osu.ppy.sh/users/${oldNominator.osuId})**")

            if (isMultipleGamemodes == true && gamemode != null) {
                nominatorChangesText.append(" [${gamemode.toReadableName()}]")
            }
        }

        return nominatorChangesText.toString()
    }

    fun logAiessUpdatedNominator(beatmap: Beatmap, oldNominatorId: String, newNominatorId: String, gamemode: Gamemode) {
        val oldNominator = userService.findUserById(oldNominatorId)
        val newNominator = userService.findUserById(newNominatorId)

        val nominatorChangesText = getChangedNominatorText(newNominator, oldNominator)
        val message = StringBuilder()
            .append(nominatorChangesText)
            .append("\n**[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}) [${gamemode.toReadableName()}]")

        discordClient.send(
            description = message.toString(),
            color = EmbedColor.BLUE,
            thumbnail = EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            footer = EmbedFooter("Aiess"),
            confidential = false,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun getChangedNominatorText(newNominator: BeatmapNominator, oldNominator: BeatmapNominator?): String? {
        val old = oldNominator?.let { userService.findUserById(it.nominatorId) }
        val new = userService.findUserById(newNominator.nominatorId)
        return getChangedNominatorText(new, old)
            .takeIf { it.isNotBlank() }
    }

    fun logGamemodeUpdatedNominators(editor: User?, beatmapId: String, beatmapGamemode: BeatmapGamemode, oldBeatmapGamemode: BeatmapGamemode?): String? {
        val (currentFirstNominator, currentSecondNominator) = oldBeatmapGamemode?.nominators.let { it?.get(0) to it?.get(1) }
        val (newFirstNominator, newSecondNominator) = beatmapGamemode.nominators.let { it[0] to it[1] }

        val firstNominatorTextChanged = getChangedNominatorText(newFirstNominator, currentFirstNominator)
        val secondNominatorTextChanged = getChangedNominatorText(newSecondNominator, currentSecondNominator)

        if (firstNominatorTextChanged != null) {
            log.info { "[UPDATE] ${editor?.username} changed 1st nominator on (beatmap = $beatmapId, gamemode = ${beatmapGamemode.gamemode} from ${currentFirstNominator?.nominatorId} to ${newFirstNominator.nominatorId}" }
        }
        if (secondNominatorTextChanged != null) {
            log.info { "[UPDATE] ${editor?.username} changed 2nd nominator on (beatmap = $beatmapId, gamemode = ${beatmapGamemode.gamemode} from ${currentSecondNominator?.nominatorId} to ${newSecondNominator.nominatorId}" }
        }

        return if (firstNominatorTextChanged != null && secondNominatorTextChanged != null) {
            firstNominatorTextChanged + "\n" + secondNominatorTextChanged
        } else {
            firstNominatorTextChanged ?: secondNominatorTextChanged
        }
    }

    fun logUpdatedNominators(osuApiToken: String, beatmap: Beatmap, oldBeatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        val nominatorChangesText = beatmap.gamemodes.mapNotNull { beatmapGamemode ->
            val oldBeatmapGamemode = oldBeatmap.gamemodes.find { it.gamemode == beatmapGamemode.gamemode }

            logGamemodeUpdatedNominators(editor, beatmap.osuId, beatmapGamemode, oldBeatmapGamemode)
        }

        val message = StringBuilder()
            .append(nominatorChangesText.joinToString { "\n" })
            .append("\n**[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}) [${beatmap.gamemodes.map { it.gamemode.toReadableName()}}]")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.BLUE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = false,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logUpdatedNominators(
        osuApiToken: String,
        beatmap: Beatmap,
        oldNominatorId: String,
        newNominatorId: String,
        gamemode: Gamemode,
        gamemodesBeforeUpdate: List<Gamemode>
    ) {
        val editor = userService.getEditor(osuApiToken)
        val oldNominator = userService.findUserById(oldNominatorId)
        val newNominator = userService.findUserById(newNominatorId)

        // Take the biggest list of gamemodes to publish
        // On removal of a gamemode we want to publish to the deleted gamemode as well.
        // On adding of a new gamemode we want to publish to the existing ane new gamemode.
        val gamemodesToMessage = if (beatmap.gamemodes.size > gamemodesBeforeUpdate.size) {
            beatmap.gamemodes.map { it.gamemode }
        } else {
            gamemodesBeforeUpdate
        }
        val isMultipleGamemodes = gamemodesToMessage.size > 1

        val nominatorChangesText = getChangedNominatorText(newNominator, oldNominator, isMultipleGamemodes, gamemode)
        log.info { "[UPDATE] ${editor?.username} changed nominators from $oldNominatorId to $newNominatorId (beatmap = ${beatmap.osuId})" }
        val beatmapGamemodeMessagePart = gamemodesToMessage.joinToString { it.toReadableName() }

        val message = StringBuilder()
            .append(nominatorChangesText)
            .append("\n**[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**")
            .append("\nMapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}) [$beatmapGamemodeMessagePart]")

        discordClient.sendBeatmapUpdate(
            description = message.toString(),
            color = EmbedColor.BLUE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = false,
            gamemodes = gamemodesToMessage
        )
    }

    private fun convertLegacyBeatmapToBeatmap(legacyBeatmap: LegacyBeatmap): Beatmap? {
        val nominatorOne = legacyBeatmap.nominators.getOrNull(0)?.toString() ?: MISSING_USER_ID
        val nominatorTwo = legacyBeatmap.nominators.getOrNull(1)?.toString() ?: MISSING_USER_ID

        // This should never happen
        val beatmapStatus = BeatmapStatus.fromPriorityStatus(legacyBeatmap.status.toInt()) ?: return null

        val catchGamemode = BeatmapGamemode(
            gamemode = Gamemode.fruits,
            nominators = listOf(
                BeatmapNominator(nominatorOne, legacyBeatmap.nominatedByBNOne),
                BeatmapNominator(nominatorTwo, legacyBeatmap.nominatedByBNTwo),
            ),
            isReady = legacyBeatmap.nominatedByBNOne || legacyBeatmap.nominatedByBNTwo
        )

        if (legacyBeatmap._id == 0L) {
            log.info { "Found a beatmap with Id 0? ${legacyBeatmap.artist} - ${legacyBeatmap.title} by ${legacyBeatmap.mapper}" }
        }

        // TODO instead of using the legacy data, retrieve the beatmap from the osu api
        return Beatmap(
            osuId = legacyBeatmap._id.toString(),
            artist = legacyBeatmap.artist,
            title = legacyBeatmap.title,
            note = legacyBeatmap.note,
            mapper = legacyBeatmap.mapper,
            mapperId = legacyBeatmap.mapperId.toString(),
            status = beatmapStatus,
            gamemodes = listOf(catchGamemode),
            dateAdded = Instant.ofEpochSecond(legacyBeatmap.dateAdded),
            dateUpdated = Instant.ofEpochSecond(legacyBeatmap.dateUpdated),
            dateRanked = Instant.ofEpochSecond(legacyBeatmap.dateRanked)
        )
    }

    private fun Beatmap.toExposedBeatmap(): ExposedBeatmap? {
        val mapper = userService.findUserById(mapperId) ?: return null
        val preparedGamemodes = gamemodes.map { entry ->
            val gamemodeNominators = entry.nominators.mapNotNull {
                val nominatorUser = userService.findUserById(it.nominatorId)

                if (nominatorUser != null) {
                    ExposedBeatmapNominator(
                        nominator = nominatorUser,
                        hasNominated = it.hasNominated
                    )
                } else {
                    null
                }
            }

            ExposedBeatmapGamemode(
                gamemode = entry.gamemode,
                nominators = gamemodeNominators,
                isReady = entry.isReady
            )
        }

        return ExposedBeatmap(
            osuId = this.osuId,
            artist = artist,
            title = title,
            note = note,
            mapper = mapper,
            status = status,
            gamemodes = preparedGamemodes,
            dateAdded = dateAdded,
            dateUpdated = dateUpdated,
            dateRanked = dateRanked
        )
    }
}
