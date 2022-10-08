package nl.greaper.bnplanner.model

import nl.greaper.bnplanner.model.osu.MeGroup

enum class Role {
    Mapper,
    Nominator,
    Probation,
    NominationAssessment;

    companion object {
        fun fromOsuId(osuId: String): Role {
            return when (osuId) {
                MeGroup.NAT -> NominationAssessment
                MeGroup.BN -> Nominator
                MeGroup.PBN -> Probation
                else -> Mapper
            }
        }
    }
}

fun Role.toReadableName(): String {
    return when (this) {
        Role.Mapper -> "Mapper"
        Role.Nominator -> "Beatmap Nominators"
        Role.Probation -> "Beatmap Nominators (Probationary)"
        Role.NominationAssessment -> "Nomination Assessment Team"
    }
}
