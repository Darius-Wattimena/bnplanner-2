package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import org.springframework.stereotype.Service
import kotlin.random.Random

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

        syncUsers(
            osuToken,
            allUsers.filter { userId ->
                val databaseUser = userService.findUserById(userId)

                if (databaseUser != null) {
                    databaseUser.restricted == true
                } else {
                    true
                }
            }.toSet()
        )
    }

    fun syncUsers(osuToken: String, users: Set<String>) {
        val token = osuToken.removePrefix("Bearer ")
        val totalUsers = users.count()

        log.info { "Checking $totalUsers users." }

        users.forEachIndexed { index, userId ->
            val currentUser = userService.findUserById(userId)
            if (currentUser == null || currentUser.restricted == true) {
                userService.forceFindUserById(token, userId)
                log.info { "[${index + 1}/$totalUsers] User $userId synced, sleeping." }
                Thread.sleep(1_000L + Random.nextInt(0, 1000))
            } else {
                log.info { "[${index + 1}/$totalUsers] User ${currentUser.username} is already good, skipping." }
            }
        }
    }
}
