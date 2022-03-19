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
        fun fromPriorityStatus(prio: Int): BeatmapStatus? {
            return when (prio) {
                1 -> Qualified
                2 -> Bubbled
                3 -> Disqualified
                4 -> Popped
                5 -> Pending
                6 -> Ranked
                7 -> Graved
                8 -> Unfinished
                else -> null
            }
        }

        fun BeatmapStatus.toPriorityStatus(): Int {
            return when (this) {
                Qualified -> 1
                Bubbled -> 2
                Disqualified -> 3
                Popped -> 4
                Pending -> 5
                Ranked -> 6
                Graved -> 7
                Unfinished -> 8
            }
        }
    }
}
