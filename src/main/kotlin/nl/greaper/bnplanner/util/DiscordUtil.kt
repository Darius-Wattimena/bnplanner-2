package nl.greaper.bnplanner.util

import nl.greaper.bnplanner.BUBBLED_STATUS_ICON
import nl.greaper.bnplanner.DISQUALIFIED_STATUS_ICON
import nl.greaper.bnplanner.GRAVED_STATUS_ICON
import nl.greaper.bnplanner.NOMINATED_STATUS_ICON
import nl.greaper.bnplanner.PENDING_STATUS_ICON
import nl.greaper.bnplanner.POPPED_STATUS_ICON
import nl.greaper.bnplanner.RANKED_STATUS_ICON
import nl.greaper.bnplanner.UNFINISHED_STATUS_ICON
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus

fun BeatmapStatus.getEmojiIcon(): String {
    return when (this) {
        BeatmapStatus.Qualified -> NOMINATED_STATUS_ICON
        BeatmapStatus.Bubbled -> BUBBLED_STATUS_ICON
        BeatmapStatus.Disqualified -> DISQUALIFIED_STATUS_ICON
        BeatmapStatus.Popped -> POPPED_STATUS_ICON
        BeatmapStatus.Pending -> PENDING_STATUS_ICON
        BeatmapStatus.Ranked -> RANKED_STATUS_ICON
        BeatmapStatus.Graved -> GRAVED_STATUS_ICON
        BeatmapStatus.Unfinished -> UNFINISHED_STATUS_ICON
    }
}
