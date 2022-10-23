package nl.greaper.bnplanner.model.aiess

import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus

data class AiessBeatmapEvent(
    /**
     * The osu ID of the beatmap.
     */
    val beatmapId: String,

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
    val userName: String?,

    /**
     * The [Gamemode] where the nominator is being updated.
     */
    val gamemode: Gamemode?
)
