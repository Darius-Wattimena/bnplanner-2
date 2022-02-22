package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.model.UserContext
import nl.greaper.bnplanner.service.OsuService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/auth")
class AuthController(private val osuService: OsuService) {

    @PostMapping
    fun login(@RequestBody token: String): UserContext? {
        return osuService.getUserContextByToken(osuService.getToken(token))
    }
}