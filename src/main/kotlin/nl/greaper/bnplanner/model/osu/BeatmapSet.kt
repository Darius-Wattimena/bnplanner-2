package nl.greaper.bnplanner.model.osu

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
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
    val last_updated: Instant
) {
    data class CurrentNomination(
        val rulesets: List<String>,
        val reset: Boolean,
        @JsonAlias("user_id")
        val userId: Long
    )
}
