package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.ADDED_NOMINATOR_ICON
import nl.greaper.bnplanner.CHANGE_BEATMAP_NOTE_ICON
import nl.greaper.bnplanner.CREATED_BEATMAP_ICON
import nl.greaper.bnplanner.DELETED_BEATMAP_ICON
import nl.greaper.bnplanner.REMOVED_NOMINATOR_ICON
import nl.greaper.bnplanner.client.DiscordWebhookClient
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
import java.time.Instant

@Service
class BeatmapService(
    private val dataSource: BeatmapDataSource,
    private val userService: UserService,
    private val osuHttpClient: OsuHttpClient,
    private val discordClient: DiscordWebhookClient
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

    fun addBeatmap(osuApiToken: String, input: NewBeatmap) {
        val addDate = Instant.now()
        val osuBeatmap = osuHttpClient.findBeatmapWithId(osuApiToken, input.osuId, false) ?: return

        val preparedBeatmapGamemodes = input.gamemodes.map {
            BeatmapGamemode(
                gamemode = it,
                nominators = listOf(
                    BeatmapNominator("0", false),
                    BeatmapNominator("0", false)
                ),
                isReady = false
            )
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

        dataSource.insertOne(newBeatmap).also { logBeatmapAdded(osuApiToken, newBeatmap) }
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
                    Beatmap::gamemodes / BeatmapGamemode::nominators / BeatmapNominator::nominatorId eq "0"
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

    fun updateBeatmapNominator(osuApiToken: String, osuId: String, gamemode: Gamemode, oldNominator: String, newNominator: String): ExposedBeatmap? {
        val databaseBeatmap = findBeatmap(osuId) ?: return null

        val updatedBeatmap = updateBeatmapGamemode(databaseBeatmap, gamemode) { updatingGamemode ->
            val (currentFirstNominator, currentSecondNominator) = updatingGamemode.nominators.toList().let {
                it[0] to it[1]
            }

            val newNominators = if (currentFirstNominator.nominatorId == oldNominator) {
                BeatmapNominator(newNominator, false) to currentSecondNominator
            } else {
                currentFirstNominator to BeatmapNominator(newNominator, false)
            }

            val updatedGamemode = updatingGamemode.copy(
                nominators = listOf(newNominators.first, newNominators.second)
            )

            updatedGamemode
        } ?: return null

        dataSource.update(updatedBeatmap)
        logUpdatedNominators(osuApiToken, updatedBeatmap, oldNominator, newNominator, gamemode)

        return updatedBeatmap.toExposedBeatmap()
    }

    fun updateBeatmapGamemode(beatmap: Beatmap, gamemode: Gamemode, new: (old: BeatmapGamemode) -> BeatmapGamemode): Beatmap? {
        val updatingGamemode = beatmap.gamemodes.find { it.gamemode == gamemode } ?: return null

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

    fun updateBeatmapGamemode(beatmap: Beatmap, updatingGamemode: BeatmapGamemode, new: (old: BeatmapGamemode) -> BeatmapGamemode): Beatmap {
        return beatmap.copy(
            gamemodes = beatmap.gamemodes - updatingGamemode + new(updatingGamemode),
            dateUpdated = Instant.now()
        )
    }

    fun logBeatmapAdded(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)

        discordClient.sendBeatmapUpdate(
            """$CREATED_BEATMAP_ICON **Created**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}})
            """.prependIndent(),
            color = EmbedColor.GREEN,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapDelete(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)
        discordClient.sendBeatmapUpdate(
            """$DELETED_BEATMAP_ICON **Deleted**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}})
            """.prependIndent(),
            color = EmbedColor.RED,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapNoteChange(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)

        discordClient.sendBeatmapUpdate(
            """$CHANGE_BEATMAP_NOTE_ICON **Updated note**
                ```${beatmap.note.replace("""\n  +""".toRegex(), "\n")}```
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}})
            """.prependIndent(),
            color = EmbedColor.ORANGE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logBeatmapStatusChange(osuApiToken: String, beatmap: Beatmap) {
        val editor = userService.getEditor(osuApiToken)

        discordClient.sendBeatmapUpdate(
            """${beatmap.status.getEmojiIcon()} **Updated status to ${beatmap.status.name}**
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}})
            """.prependIndent(),
            color = EmbedColor.ORANGE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = true,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    private fun getChangedNominatorText(newNominator: User?, oldNominator: User?): String {
        var nominatorChangesText = ""

        if (newNominator != null && newNominator.osuId != "0") {
            nominatorChangesText = "$ADDED_NOMINATOR_ICON **Added [${newNominator.username}](https://osu.ppy.sh/users/${newNominator.osuId})**"
        }

        if (oldNominator != null && oldNominator.osuId != "0") {
            val optionalNewLine = if (nominatorChangesText != "") {
                // Add a \n if we added a nominator
                "\n"
            } else {
                ""
            }

            nominatorChangesText += "$optionalNewLine$REMOVED_NOMINATOR_ICON **Removed [${oldNominator.username}](https://osu.ppy.sh/users/${oldNominator.osuId})**"
        }

        return nominatorChangesText
    }

    fun logAiessUpdatedNominator(beatmap: Beatmap, oldNominatorId: String, newNominatorId: String, gamemode: Gamemode) {
        val oldNominator = userService.findUserById(oldNominatorId)
        val newNominator = userService.findUserById(newNominatorId)

        val nominatorChangesText = getChangedNominatorText(newNominator, oldNominator)

        discordClient.send(
            """$nominatorChangesText
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}}) [${gamemode.toReadableName()}]
            """.prependIndent(),
            color = EmbedColor.BLUE,
            thumbnail = EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            footer = EmbedFooter("Aiess"),
            confidential = false,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    fun logUpdatedNominators(osuApiToken: String, beatmap: Beatmap, oldNominatorId: String, newNominatorId: String, gamemode: Gamemode) {
        val editor = userService.getEditor(osuApiToken)
        val oldNominator = userService.findUserById(oldNominatorId)
        val newNominator = userService.findUserById(newNominatorId)

        val nominatorChangesText = getChangedNominatorText(newNominator, oldNominator)

        discordClient.sendBeatmapUpdate(
            """$nominatorChangesText
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${beatmap.mapper}](https://osu.ppy.sh/users/${beatmap.mapperId}}) [${gamemode.toReadableName()}]
            """.prependIndent(),
            color = EmbedColor.BLUE,
            beatmapId = beatmap.osuId,
            editor = editor,
            confidential = false,
            gamemodes = beatmap.gamemodes.map { it.gamemode }
        )
    }

    private fun convertLegacyBeatmapToBeatmap(legacyBeatmap: LegacyBeatmap): Beatmap? {
        val nominatorOne = legacyBeatmap.nominators.getOrNull(0)?.toString() ?: "0"
        val nominatorTwo = legacyBeatmap.nominators.getOrNull(1)?.toString() ?: "0"

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
