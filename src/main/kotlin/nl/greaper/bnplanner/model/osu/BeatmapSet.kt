package nl.greaper.bnplanner.model.osu

import java.time.Instant

data class BeatmapSet(
    val id: String,
    val title: String,
    val artist: String,
    val creator: String,
    val user_id: String,
    val ranked: Int,
    val ranked_date: Instant? = null,
    val last_updated: Instant
)
