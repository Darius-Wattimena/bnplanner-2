package nl.greaper.bnplanner.model.beatmap

enum class BeatmapStatus {
    Qualified,
    Bubbled,
    Disqualified,
    Popped,
    Pending,
    Ranked,
    Graved,
    Unfinished;

    companion object {
        fun fromLegacyStatus(legacyStatus: Long): BeatmapStatus?
        {
            return when (legacyStatus) {
                1L -> Qualified
                2L -> Bubbled
                3L -> Disqualified
                4L -> Popped
                5L -> Pending
                6L -> Ranked
                7L -> Graved
                8L -> Unfinished
                else -> null
            }
        }
    }
}