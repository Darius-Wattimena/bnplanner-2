package nl.greaper.bnplanner.model.beatmap

enum class BeatmapStatus {
    Qualified,
    Nominated,
    Disqualified,
    Reset,
    Pending,
    Ranked,
    Graved,
    Unfinished;

    companion object {
        fun fromPriorityStatus(prio: Int): BeatmapStatus? {
            return when (prio) {
                1 -> Qualified
                2 -> Nominated
                3 -> Disqualified
                4 -> Reset
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
                Nominated -> 2
                Disqualified -> 3
                Reset -> 4
                Pending -> 5
                Ranked -> 6
                Graved -> 7
                Unfinished -> 8
            }
        }
    }
}
