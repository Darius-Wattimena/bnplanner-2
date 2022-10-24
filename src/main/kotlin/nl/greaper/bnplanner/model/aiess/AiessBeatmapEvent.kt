package nl.greaper.bnplanner.model.aiess

import com.fasterxml.jackson.annotation.JsonProperty
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus

data class AiessBeatmapEvent(
    /**
     * The osu ID of the beatmap.
     */
    val beatmapSetId: String,

    /**
     * The artist of the song currently known by osu
     */
    val artist: String,

    /**
     * The title of the song currently known by osu
     */
    val title: String,

    /**
     * The current username of the mapper
     */
    @JsonProperty("creatorName")
    val mapper: String,

    /**
     * The osu ID of the user
     */
    @JsonProperty("creatorId")
    val mapperId: String,

    /**
     * The current status of the beatmap.
     *
     * @see BeatmapStatus
     */
    val status: BeatmapStatus,

    /**
     * The osu ID of the user updating the status of the beatmap.
     * This is the nominator in the case of a nomination.
     */
    val userId: String?,

    /**
     * The current username of the user in osu.
     */
    @JsonProperty("userName")
    val username: String?,

    /**
     * The [Gamemode] where the nominator is being updated.
     */
    val modes: List<Gamemode>
)
