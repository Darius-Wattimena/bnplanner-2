package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import org.springframework.stereotype.Service

@Service
class FixService(
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService
) {
    val log = KotlinLogging.logger { }

    fun syncUsers(osuToken: String) {
        val allBeatmaps = beatmapDataSource.list()
        val allUsers = mutableSetOf<String>()

        allBeatmaps.forEach { beatmap ->
            beatmap.gamemodes.forEach { gamemode ->
                gamemode.nominators.forEach {
                    allUsers.add(it.nominatorId)
                }
            }

            allUsers.add(beatmap.mapperId)
        }

        val token = osuToken.removePrefix("Bearer ")
        val totalUsers = allUsers.count()

        log.info { "Checking $totalUsers users." }

        allUsers.forEachIndexed { index, userId ->
            val currentUser = userService.findUserById(userId)
            if (currentUser == null || currentUser.restricted == true) {
                userService.forceFindUserById(token, userId)
                log.info { "[${index + 1}/$totalUsers] User $userId synced, sleeping." }
                Thread.sleep(20_000)
            } else {
                log.info { "[${index + 1}/$totalUsers] User ${currentUser.username} is already good, skipping." }
            }
        }
    }
}
