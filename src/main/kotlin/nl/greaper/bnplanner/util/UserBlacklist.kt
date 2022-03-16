package nl.greaper.bnplanner.util

val RESTRICTED_IDS = setOf(
    "3071175",
    "2697284",
    "6694242"
)

// TODO should be done in the database?
fun shouldSkipUser(userOsuId: String): Boolean {
    return RESTRICTED_IDS.contains(userOsuId)
}
