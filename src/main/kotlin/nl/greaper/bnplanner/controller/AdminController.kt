package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.service.FixService
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.security.RolesAllowed

@RestController
@RequestMapping("/v2/admin")
class AdminController(
    private val fixService: FixService
) {
    @PostMapping("/sync/users")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun syncAllUsers(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String
    ) {
        fixService.syncAllUsers(osuApiToken)
    }

    @PostMapping("/sync/users/ids")
    @RolesAllowed(RolePermission.ADMIN)
    fun syncUsers(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam(required = false) force: Boolean?,
        @RequestBody users: Set<String>
    ) {
        fixService.syncUsers(osuApiToken, users, force ?: false)
    }

    @GetMapping("/sync/beatmaps/pending")
    @RolesAllowed(RolePermission.ADMIN)
    fun syncBeatmapsByStatus(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam(required = false) status: BeatmapStatus? = null
    ) {
        if (status == null) {
            fixService.syncAllBeatmaps(osuApiToken, BeatmapPage.PENDING)
        } else {
            fixService.syncBeatmaps(osuApiToken, BeatmapPage.PENDING, status)
        }
    }

    @PostMapping("/sync/beatmaps/graveyard")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun syncAllGraveyardBeatmaps(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String
    ) {
        fixService.syncAllBeatmaps(osuApiToken, BeatmapPage.GRAVEYARD)
    }
}
