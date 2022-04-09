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

    @PostMapping("/refresh")
    fun refreshLogin(@RequestBody refreshToken: String): UserContext? {
        val parsedToken = refreshToken.dropLast(1) // Somehow frontend always sends a trailing '='
        val authToken = osuService.getAuthTokenByRefreshToken(parsedToken)
        return osuService.getUserContextByToken(authToken)
    }
}
