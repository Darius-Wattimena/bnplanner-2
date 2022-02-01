package nl.greaper.bnplanner.model.beatmap

import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.User
import java.time.Instant

data class ExposedBeatmap(
    val osuId: String,
    val artist: String,
    val title: String,
    val note: String,
    val mapper: User,
    val status: BeatmapStatus,
    val gamemodes: List<ExposedBeatmapGamemode>,

    val dateAdded: Instant,
    val dateUpdated: Instant,
    val dateRanked: Instant?
)

data class ExposedBeatmapGamemode(
    val gamemode: Gamemode,
    val nominators: List<ExposedBeatmapNominator>,
    val isReady: Boolean
)

data class ExposedBeatmapNominator(
    val nominator: User,
    val hasNominated: Boolean
)