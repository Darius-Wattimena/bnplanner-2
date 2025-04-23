package nl.greaper.bnplanner.util

import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode

fun BeatmapGamemode.hasAnyNomination(): Boolean = this.nominators.any { nominator -> nominator.hasNominated }

fun BeatmapGamemode.hasAllNominations(): Boolean = this.nominators.all { nominator -> nominator.hasNominated }
