package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.datasource.BeatmapDataSource
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import org.springframework.stereotype.Service
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Service
class FixService(
    private val beatmapDataSource: BeatmapDataSource,
    private val userService: UserService,
    private val beatmapService: BeatmapService
) {
    val log = KotlinLogging.logger { }

    fun syncAllBeatmaps(osuToken: String, page: BeatmapPage): SyncInfo {
        return syncBeatmaps(
            osuToken = osuToken,
            page = page,
            status = emptySet()
        )
    }

    fun syncBeatmaps(osuToken: String, page: BeatmapPage, status: Set<BeatmapStatus>): SyncInfo {
        val beatmaps = beatmapService.findBeatmapsIds(
            search = null,
            artist = null,
            title = null,
            mapper = null,
            status = status,
            nominators = emptySet(),
            page = page,
            gamemodes = emptySet(),
            missingNominator = emptySet()
        )

        return syncBeatmaps(osuToken, beatmaps)
    }

    fun syncBeatmaps(osuToken: String, beatmaps: Set<String>): SyncInfo {
        val totalBeatmaps = beatmaps.count()

        log.info { "Checking $totalBeatmaps beatmaps." }

        val duration = measureTimeMillis {
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

        return SyncInfo(
            duration = duration,
            totalSynced = totalBeatmaps
        )
    }

    fun syncAllUsers(osuToken: String): SyncInfo {
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

        val unrestrictedUsers = allUsers.filter { userId ->
            val databaseUser = userService.findUserById(userId)

            if (databaseUser != null) {
                databaseUser.restricted != true
            } else {
                true
            }
        }.toSet()

        return syncUsers(
            osuToken = osuToken,
            users = unrestrictedUsers,
            force = true
        )
    }

    fun syncUsers(osuToken: String, users: Set<String>, force: Boolean = false): SyncInfo {
        val token = osuToken.removePrefix("Bearer ")
        val totalUsers = users.count()

        log.info { "Checking $totalUsers users." }

        val duration = measureTimeMillis {
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

        return SyncInfo(
            duration = duration,
            totalSynced = totalUsers
        )
    }

    data class SyncInfo(
        val duration: Long,
        val totalSynced: Int
    )
}
