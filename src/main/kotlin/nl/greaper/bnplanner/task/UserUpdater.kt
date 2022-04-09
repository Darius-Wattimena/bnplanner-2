package nl.greaper.bnplanner.task

import nl.greaper.bnplanner.datasource.OsuTokenDataSource
import nl.greaper.bnplanner.datasource.UserRecalculateDataSource
import nl.greaper.bnplanner.service.OsuService
import nl.greaper.bnplanner.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["recalculation.active"], havingValue = "true", matchIfMissing = false)
class UserUpdater(
    private val osuService: OsuService,
    private val userService: UserService,
    private val userRecalculateDataSource: UserRecalculateDataSource,
    private val osuTokenDataSource: OsuTokenDataSource
) {
    @Scheduled(cron = "*/15 * * * * *")
    fun taskUserUpdater() {
        val recalculationUser = userRecalculateDataSource.findFirst() ?: return
        val authToken = osuTokenDataSource.findFirst() ?: return
        val accessToken = osuService.getValidUpdaterToken(authToken.access_token, authToken.refresh_token) ?: return
        val recalculatedUser = userService.forceFindUserFromId(accessToken, recalculationUser.osuId) ?: return

        userRecalculateDataSource.deleteById(recalculatedUser.osuId)
    }
}