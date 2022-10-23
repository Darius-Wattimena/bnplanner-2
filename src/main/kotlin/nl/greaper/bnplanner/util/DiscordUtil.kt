package nl.greaper.bnplanner.util

import nl.greaper.bnplanner.DISQUALIFY_STATUS_ICON
import nl.greaper.bnplanner.GRAVED_STATUS_ICON
import nl.greaper.bnplanner.NOMINATE_STATUS_ICON
import nl.greaper.bnplanner.PENDING_STATUS_ICON
import nl.greaper.bnplanner.QUALIFY_STATUS_ICON
import nl.greaper.bnplanner.RANKED_STATUS_ICON
import nl.greaper.bnplanner.RESET_STATUS_ICON
import nl.greaper.bnplanner.UNFINISHED_STATUS_ICON
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus

fun BeatmapStatus.getEmojiIcon(): String {
    return when (this) {
        BeatmapStatus.Qualified -> QUALIFY_STATUS_ICON
        BeatmapStatus.Nominated -> NOMINATE_STATUS_ICON
        BeatmapStatus.Disqualified -> DISQUALIFY_STATUS_ICON
        BeatmapStatus.Reset -> RESET_STATUS_ICON
        BeatmapStatus.Pending -> PENDING_STATUS_ICON
        BeatmapStatus.Ranked -> RANKED_STATUS_ICON
        BeatmapStatus.Graved -> GRAVED_STATUS_ICON
        BeatmapStatus.Unfinished -> UNFINISHED_STATUS_ICON
    }
}

fun Gamemode.toReadableName(): String {
    return when (this) {
        Gamemode.osu -> "osu"
        Gamemode.taiko -> "taiko"
        Gamemode.fruits -> "catch"
        Gamemode.mania -> "mania"
    }
}
