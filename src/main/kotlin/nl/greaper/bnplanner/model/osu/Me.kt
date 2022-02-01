package nl.greaper.bnplanner.model.osu

data class Me(
        val id: String,
        val username: String,
        val groups: List<MeGroup>? = null
)