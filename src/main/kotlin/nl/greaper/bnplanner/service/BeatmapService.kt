package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapNominator
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.LegacyBeatmap
import nl.greaper.bnplanner.model.beatmap.NewBeatmap
import nl.greaper.bnplanner.util.quote
import org.bson.conversions.Bson
import org.litote.kmongo.`in`
import org.litote.kmongo.and
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.ne
import org.litote.kmongo.or
import org.litote.kmongo.regex
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class BeatmapService(
    private val dataSource: BeatmapDataSource,
    private val osuHttpClient: OsuHttpClient
) {
    fun findBeatmap(id: String): Beatmap? {
        return dataSource.findById(id)
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
        artist: String?,
        title: String?,
        mapper: String?,
        status: Set<BeatmapStatus>,
        nominators: Set<String>,
        page: BeatmapPage,
        from: Int,
        to: Int
    ): List<Beatmap> {
        return dataSource.findAll(setupFilter(artist, title, mapper, status, nominators, page), from, to)
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
        }.associateBy { it.gamemode }

        val newBeatmap = Beatmap(
            osuId = newBeatmap.osuId,
            artist = osuBeatmap.artist,
            title = osuBeatmap.title,
            note = "",
            mapperId = osuBeatmap.user_id,
            status = BeatmapStatus.Pending,
            gamemodes = preparedBeatmapGamemodes,
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

        filters += if (status.isNotEmpty()) {
            or(status.map { Beatmap::status eq it })
        } else {
            when (page) {
                BeatmapPage.PENDING -> and(
                    Beatmap::status ne BeatmapStatus.Ranked,
                    Beatmap::status ne BeatmapStatus.Graved
                )
                BeatmapPage.RANKED -> Beatmap::status eq BeatmapStatus.Ranked
                BeatmapPage.GRAVEYARD -> Beatmap::status eq BeatmapStatus.Graved
            }
        }

        return and(filters)
    }

    fun importLegacyBeatmaps(legacyBeatmaps: List<LegacyBeatmap>) {
        val convertedBeatmaps = legacyBeatmaps.mapNotNull { convertLegacyBeatmapToBeatmap(it) }
        dataSource.insertMany(convertedBeatmaps)
    }

    private fun convertLegacyBeatmapToBeatmap(legacyBeatmap: LegacyBeatmap): Beatmap? {
        // Don't convert if the beatmap had a user which could not be found (e.g. restricted person)
        val nominatorOne = legacyBeatmap.nominators.getOrNull(0)?.toString()?.takeIf { it != "0" }
        val nominatorTwo = legacyBeatmap.nominators.getOrNull(1)?.toString()?.takeIf { it != "0" }

        // This should never happen
        val beatmapStatus = BeatmapStatus.fromLegacyStatus(legacyBeatmap.status) ?: return null

        val catchGamemode = BeatmapGamemode(
            gamemode = Gamemode.fruits,
            nominators = setOfNotNull(
                nominatorOne?.let { BeatmapNominator(it, legacyBeatmap.nominatedByBNOne) },
                nominatorTwo?.let { BeatmapNominator(it, legacyBeatmap.nominatedByBNTwo) },
            ),
            isReady = legacyBeatmap.nominatedByBNOne || legacyBeatmap.nominatedByBNTwo
        )

        return Beatmap(
            osuId = legacyBeatmap.osuId.toString(),
            artist = legacyBeatmap.artist,
            title = legacyBeatmap.title,
            note = legacyBeatmap.note,
            mapperId = legacyBeatmap.mapperId.toString(),
            status = beatmapStatus,
            gamemodes = mapOf(Gamemode.fruits to catchGamemode),
            dateAdded = Instant.ofEpochSecond(legacyBeatmap.dateAdded),
            dateUpdated = Instant.ofEpochSecond(legacyBeatmap.dateUpdated),
            dateRanked = Instant.ofEpochSecond(legacyBeatmap.dateRanked)
        )
    }

    private fun mergeGamemodes(oldMap: Map<Gamemode, BeatmapGamemode>, newMap: Map<Gamemode, BeatmapGamemode>): Map<Gamemode, BeatmapGamemode> {
        return Gamemode.values().mapNotNull { existingGamemode ->
            val oldGamemode = oldMap[existingGamemode]
            val newGamemode = newMap[existingGamemode]

            if (newGamemode != null) {
                existingGamemode to newGamemode
            } else if (oldGamemode != null) {
                existingGamemode to oldGamemode
            } else {
                null
            }
        }.toMap()
    }

    fun updateBeatmap(osuId: String, gamemodes: Map<Gamemode, BeatmapGamemode>) {
        val databaseBeatmap = dataSource.findById(osuId) ?: return
        val databaseGamemodes = databaseBeatmap.gamemodes
        val mergedGamemodes = mergeGamemodes(databaseGamemodes, gamemodes)
        dataSource.updateBeatmapGamemodes(databaseBeatmap.osuId, mergedGamemodes)
    }
}