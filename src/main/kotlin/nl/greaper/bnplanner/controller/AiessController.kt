package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.aiess.AiessBeatmapEvent
import nl.greaper.bnplanner.service.AiessService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.security.RolesAllowed

@RestController
@RequestMapping("/v2/aiess")
class AiessController(
    private val service: AiessService
) {
    @PostMapping("/event/beatmap")
    @RolesAllowed(RolePermission.BOT, RolePermission.DEVELOPER)
    fun beatmapEvent(@RequestBody body: AiessBeatmapEvent): Boolean {
        return service.processAiessBeatmapEvent(body)
    }
}