package nl.greaper.bnplanner.model.osu

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import nl.greaper.bnplanner.model.Gamemode
import java.time.Instant

data class BeatmapSet(
    val id: String,
    val title: String,
    val artist: String,
    val creator: String,
    val user_id: String,
    @JsonProperty("current_nominations")
    val currentNominations: List<CurrentNomination> = emptyList(),
    val ranked: Int,
    val ranked_date: Instant? = null,
    val last_updated: Instant,
    val beatmaps: List<Beatmap> = emptyList(),
) {
    data class CurrentNomination(
        val rulesets: List<String>,
        val reset: Boolean,
        @JsonAlias("user_id")
        val userId: Long,
    )

    data class Beatmap(
        val total_length: Int,
        val user_id: Int,
        val mode: Gamemode,
        val version: String,
    )
}
