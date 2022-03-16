package nl.greaper.bnplanner.model.osu

data class MeGroup(
    val id: String,
    val playmodes: List<String>?
) {

    companion object {
        val NAT = "7"
        val BN = "28"
        val PBN = "32"
        val LVD = "31"
        val SupportedGroups = setOf(NAT, BN, PBN)
    }
}
