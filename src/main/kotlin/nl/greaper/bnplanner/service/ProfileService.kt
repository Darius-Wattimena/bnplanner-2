package nl.greaper.bnplanner.service

import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.profile.ProfileStatisticsPairInfo
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val beatmapService: BeatmapService,
    private val userService: UserService
) {
    fun getUserPairing(userId: String): List<ProfileStatisticsPairInfo> {
        val beatmaps = beatmapService.findBeatmapsByNominator(userId)

        val otherNominatorCount = mutableMapOf<String, Count>()

        beatmaps.forEach { beatmap ->
            beatmap.gamemodes.forEach { beatmapGamemode ->
                // Only count when the nominator id is equal to the userId we want to count for
                if (beatmapGamemode.nominators.any { it.nominatorId == userId }) {
                    val firstNominator = beatmapGamemode.nominators[0].nominatorId
                    val secondNominator = beatmapGamemode.nominators[1].nominatorId

                    countOtherNominator(beatmap.status, userId, firstNominator, secondNominator, otherNominatorCount)
                }
            }
        }

        return otherNominatorCount.toProfileStatisticsPairInfo().sortedBy { it.name }
    }

    private data class Count(
        var pending: Int,
        var ranked: Int,
        var graveyard: Int
    )

    private fun Map<String, Count>.toProfileStatisticsPairInfo(): List<ProfileStatisticsPairInfo> {
        return this.mapNotNull { (userId, amount) ->
            val user = userService.findUserById(userId)

            if (user != null) {
                ProfileStatisticsPairInfo(
                    name = user.username,
                    pairingPending = amount.pending,
                    pairingRanked = amount.ranked,
                    pairingGraved = amount.graveyard
                )
            } else {
                null
            }
        }
    }

    private fun countOtherNominator(
        beatmapStatus: BeatmapStatus,
        userId: String,
        firstNominator: String,
        secondNominator: String,
        currentCounts: MutableMap<String, Count>
    ) {
        val countingField = when (beatmapStatus) {
            BeatmapStatus.Ranked -> Count::ranked
            BeatmapStatus.Graved -> Count::graveyard
            else -> Count::pending
        }

        if (firstNominator == userId) {
            if (secondNominator != "0") {
                currentCounts.compute(secondNominator) { _, current ->
                    val actualCurrent = current ?: Count(0, 0, 0)
                    val newCount = countingField.get(actualCurrent).inc()
                    countingField.set(actualCurrent, newCount)
                    actualCurrent
                }
            }
        } else if (secondNominator == userId) {
            if (firstNominator != "0") {
                currentCounts.compute(firstNominator) { _, current ->
                    val actualCurrent = current ?: Count(0, 0, 0)
                    val newCount = countingField.get(actualCurrent).inc()
                    countingField.set(actualCurrent, newCount)
                    actualCurrent
                }
            }
        }
    }
}