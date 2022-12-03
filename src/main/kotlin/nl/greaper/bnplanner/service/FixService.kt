package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class FixService(
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService,
    private val beatmapService: BeatmapService
) {
    val log = KotlinLogging.logger { }

    fun syncBeatmaps(osuToken: String, status: BeatmapStatus) {
        beatmapService.findBeatmaps(
            osuApiToken = osuToken,
            artist = null,
            title = null,
            mapper = null,
            status = setOf(status),
            nominators = emptySet(),
            page = BeatmapPage.PENDING,
            from = 0,
            to = 9999,
            gamemodes = emptySet(),
            missingNominator = emptySet()
        )
    }

    fun syncBeatmaps(osuToken: String, beatmaps: Set<String>) {
        val totalBeatmaps = beatmaps.count()

        log.info { "Checking $totalBeatmaps beatmaps." }

        beatmaps.forEachIndexed { index, beatmapId ->
            val beatmap = beatmapService.findBeatmap(beatmapId)

            // Remove the users when we already know it
            if (beatmap != null) {
                beatmapService.syncBeatmap(osuToken, beatmap)

                log.info { "[${index + 1}/$totalBeatmaps] Beatmap ${beatmap.osuId} synced, sleeping." }
                Thread.sleep(1_000L + Random.nextInt(0, 1000))
            }
        }
    }

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

    fun syncUsers(osuToken: String, users: Set<String>, force: Boolean = false) {
        val token = osuToken.removePrefix("Bearer ")
        val totalUsers = users.count()

        log.info { "Checking $totalUsers users." }

        users.forEachIndexed { index, userId ->
            val currentUser = userService.findUserById(userId)
            if (force || currentUser == null || currentUser.restricted == true) {
                // Remove the users when we already know it
                if (force && currentUser != null) {
                    userService.deleteUser(currentUser)
                }

                userService.forceFindUserById(token, userId)
                log.info { "[${index + 1}/$totalUsers] User $userId synced, sleeping." }
                Thread.sleep(1_000L + Random.nextInt(0, 1000))
            } else {
                log.info { "[${index + 1}/$totalUsers] User ${currentUser.username} is already good, skipping." }
            }
        }
    }
}
