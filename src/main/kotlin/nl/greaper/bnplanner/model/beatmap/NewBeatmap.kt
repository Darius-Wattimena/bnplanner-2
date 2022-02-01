package nl.greaper.bnplanner.model.beatmap

import nl.greaper.bnplanner.model.Gamemode

data class NewBeatmap(
    val osuId: String,
    val gamemodes: Set<Gamemode>
)
