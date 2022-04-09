package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.ADDED_NOMINATOR_ICON
import nl.greaper.bnplanner.REMOVED_NOMINATOR_ICON
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.PageLimit
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
import nl.greaper.bnplanner.util.quote
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

    fun findBeatmap(osuApiToken: String, id: String): ExposedBeatmap? {
        return dataSource.findById(id)?.toExposedBeatmap(osuApiToken)
    }

    fun deleteBeatmap(osuId: String) {
        dataSource.deleteById(osuId)
    }

    fun countBeatmaps(
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage
    ): Int {
        return dataSource.count(setupFilter(artist, title, mapper, status, nominators, page))
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
        to: Int
    ): List<ExposedBeatmap> {
        return dataSource.findAll(
            setupFilter(artist, title, mapper, status, nominators, page),
            from,
            to
        ).mapNotNull { it.toExposedBeatmap(osuApiToken) }
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
        pageLimit: PageLimit
    ): List<ExposedBeatmap> {
        return dataSource.findAll(
            setupFilter(artist, title, mapper, status, nominators, page),
            pageNumber,
            pageLimit
        ).mapNotNull { it.toExposedBeatmap(osuApiToken) }
    }

    fun addBeatmap(osuApiToken: String, newBeatmap: NewBeatmap) {
        val addDate = Instant.now()
        val osuBeatmap = osuHttpClient.findBeatmapWithId(osuApiToken, newBeatmap.osuId) ?: return

        val preparedBeatmapGamemodes = newBeatmap.gamemodes.map {
            BeatmapGamemode(
                gamemode = it,
                nominators = emptySet(),
                isReady = false
            )
        }

        val newBeatmap = Beatmap(
            osuId = newBeatmap.osuId,
            artist = osuBeatmap.artist,
            title = osuBeatmap.title,
            note = "",
            mapperId = osuBeatmap.user_id,
            status = BeatmapStatus.Pending,
            gamemodes = preparedBeatmapGamemodes.toSet(),
            dateAdded = addDate,
            dateUpdated = addDate,
            dateRanked = null
        )

        dataSource.insertOne(newBeatmap)
    }

    private fun setupFilter(
        artist: String?,
        title: String?,
        mapperId: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage
    ): Bson {
        val filters = mutableListOf<Bson>()

        artist?.let { filters += Beatmap::artist regex quote(it).toRegex(RegexOption.IGNORE_CASE) }
        title?.let { filters += Beatmap::title regex quote(it).toRegex(RegexOption.IGNORE_CASE) }
        mapperId?.let { filters += Beatmap::mapperId eq it }

        if (nominators.isNotEmpty()) {
            filters += Beatmap::gamemodes / BeatmapGamemode::nominators / BeatmapNominator::nominatorId `in` nominators
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

    fun updateBeatmap(osuApiToken: String, osuId: String, gamemode: Gamemode, oldNominator: String, newNominator: String): ExposedBeatmap? {
        val databaseBeatmap = dataSource.findById(osuId)
        val updatingGamemode = databaseBeatmap?.gamemodes?.find { it.gamemode == gamemode }

        if (updatingGamemode != null) {
            // Don't update anything if we don't have the deleting nominator or already have the new nominator
            if (updatingGamemode.nominators.any { it.nominatorId == newNominator }
                || updatingGamemode.nominators.all { it.nominatorId != oldNominator }) {
                return null
            }

            val newNominators = updatingGamemode.nominators
                .filterNot { it.nominatorId == oldNominator }

            val updatedGamemode = updatingGamemode.copy(
                nominators = (newNominators + BeatmapNominator(newNominator, false)).toSet()
            )

            val updatedGamemodes = databaseBeatmap.gamemodes
            val updatedBeatmap = databaseBeatmap.copy(
                gamemodes = updatedGamemodes - updatingGamemode + updatedGamemode
            )

            dataSource.update(updatedBeatmap)
            logUpdatedNominators(osuApiToken, updatedBeatmap, oldNominator, newNominator)

            return updatedBeatmap.toExposedBeatmap(osuApiToken)
        }

        return null
    }

    fun logUpdatedNominators(osuApiToken: String, beatmap: Beatmap, oldNominatorId: String, newNominatorId: String) {
        val editor = userService.getEditor(osuApiToken)
        val oldNominator = userService.findUserFromId(osuApiToken, oldNominatorId)
        val newNominator = userService.findUserFromId(osuApiToken, newNominatorId)
        val mapper = userService.findUserFromId(osuApiToken, beatmap.mapperId)

        var nominatorChangesText = "$ADDED_NOMINATOR_ICON **Added [${newNominator?.username}](https://osu.ppy.sh/users/${newNominator?.osuId})**\n"
        nominatorChangesText += "$REMOVED_NOMINATOR_ICON **Removed [${oldNominator?.username}](https://osu.ppy.sh/users/${oldNominator?.osuId})**"

        discordClient.send(
            """$nominatorChangesText
                **[${beatmap.artist} - ${beatmap.title}](https://osu.ppy.sh/beatmapsets/${beatmap.osuId})**
                Mapped by [${mapper?.username}](https://osu.ppy.sh/users/${mapper?.osuId}})
            """.prependIndent(),
            color = EmbedColor.BLUE,
            thumbnail = EmbedThumbnail("https://b.ppy.sh/thumb/${beatmap.osuId}l.jpg"),
            footer = EmbedFooter(editor?.username ?: "", "https://a.ppy.sh/${editor?.osuId}"),
            confidential = false
        )
    }

    private fun convertLegacyBeatmapToBeatmap(legacyBeatmap: LegacyBeatmap): Beatmap? {
        // Don't convert if the beatmap had a user which could not be found (e.g. restricted person)
        val nominatorOne = legacyBeatmap.nominators.getOrNull(0)?.toString()?.takeIf { it != "0" }
        val nominatorTwo = legacyBeatmap.nominators.getOrNull(1)?.toString()?.takeIf { it != "0" }

        // This should never happen
        val beatmapStatus = BeatmapStatus.fromPriorityStatus(legacyBeatmap.status.toInt()) ?: return null

        val catchGamemode = BeatmapGamemode(
            gamemode = Gamemode.fruits,
            nominators = setOfNotNull(
                nominatorOne?.let { BeatmapNominator(it, legacyBeatmap.nominatedByBNOne) },
                nominatorTwo?.let { BeatmapNominator(it, legacyBeatmap.nominatedByBNTwo) },
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
            mapperId = legacyBeatmap.mapperId.toString(),
            status = beatmapStatus,
            gamemodes = setOf(catchGamemode),
            dateAdded = Instant.ofEpochSecond(legacyBeatmap.dateAdded),
            dateUpdated = Instant.ofEpochSecond(legacyBeatmap.dateUpdated),
            dateRanked = Instant.ofEpochSecond(legacyBeatmap.dateRanked)
        )
    }

    private fun Beatmap.toExposedBeatmap(osuApiToken: String): ExposedBeatmap? {
        val mapper = userService.findUserFromId(osuApiToken, mapperId) ?: return null
        val preparedGamemodes = gamemodes.map { entry ->
            val gamemodeNominators = entry.nominators.mapNotNull {
                val nominatorUser = userService.findUserFromId(osuApiToken, it.nominatorId)

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
