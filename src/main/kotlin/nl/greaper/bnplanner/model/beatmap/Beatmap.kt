package nl.greaper.bnplanner.model.beatmap

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.util.BeatmapStatusDeserializer
import nl.greaper.bnplanner.util.BeatmapStatusSerializer
import org.bson.codecs.pojo.annotations.BsonId
import java.time.Instant

data class Beatmap(
    @BsonId
    val osuId: String,
    val artist: String,
    val title: String,
    val note: String,
    val mapperId: String,
    @JsonSerialize(using = BeatmapStatusSerializer::class)
    @JsonDeserialize(using = BeatmapStatusDeserializer::class)
    val status: BeatmapStatus,
    val gamemodes: Map<Gamemode, BeatmapGamemode>,

    val dateAdded: Instant,
    val dateUpdated: Instant,
    val dateRanked: Instant?
)

data class BeatmapGamemode(
    val gamemode: Gamemode,
    val nominators: Set<BeatmapNominator>,
    val isReady: Boolean
)

data class BeatmapNominator(
    val nominatorId: String,
    val hasNominated: Boolean
)
