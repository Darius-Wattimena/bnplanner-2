package nl.greaper.bnplanner.model

import org.bson.codecs.pojo.annotations.BsonId
import java.time.Instant

data class Beatmap(
    @BsonId
    val osuId: String,
    val artist: String,
    val title: String,
    val note: String,
    val mapperId: String,
    val status: BeatmapStatus,
    val gamemodes: List<GamemodeBeatmap>,

    val dateAdded: Instant,
    val dateUpdated: Instant,
    val dateRanked: Instant
)

data class GamemodeBeatmap(
    val gamemode: Gamemode,
    val nominators: List<BeatmapNominator>,
    val isReady: Boolean
)

data class BeatmapNominator(
    val nominatorId: String,
    val hasNominated: Boolean
)

