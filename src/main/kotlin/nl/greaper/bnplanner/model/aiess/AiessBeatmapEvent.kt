package nl.greaper.bnplanner.model.aiess

import nl.greaper.bnplanner.model.beatmap.BeatmapStatus

data class AiessBeatmapEvent(
    val osuId: String,
    val nominatorOsuId: String,
    val status: BeatmapStatus
)
