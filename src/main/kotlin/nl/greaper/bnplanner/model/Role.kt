package nl.greaper.bnplanner.model

import nl.greaper.bnplanner.model.osu.MeGroup

enum class Role {
    Mapper,
    Nominator,
    Probation,
    NominationAssessment,
    Loved;

    companion object {
        fun fromOsuId(osuId: String): Role {
            return when (osuId) {
                MeGroup.NAT -> NominationAssessment
                MeGroup.BN -> Nominator
                MeGroup.PBN -> Probation
                MeGroup.LVD -> Loved
                else -> Mapper
            }
        }
    }
}
