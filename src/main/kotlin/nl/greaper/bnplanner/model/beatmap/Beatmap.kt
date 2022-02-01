package nl.greaper.bnplanner.model.beatmap

import nl.greaper.bnplanner.model.Gamemode
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
    val gamemodes: Map<Gamemode, BeatmapGamemode>,

    val dateAdded: Instant,
    val dateUpdated: Instant,
    val dateRanked: Instant?
)

data class BeatmapGamemode(
    val gamemode: Gamemode,
    val nominators: List<BeatmapNominator>,
    val isReady: Boolean
)

data class BeatmapNominator(
    val nominatorId: String,
    val hasNominated: Boolean
)

