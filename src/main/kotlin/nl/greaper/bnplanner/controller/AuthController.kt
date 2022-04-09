package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.model.UserContext
import nl.greaper.bnplanner.service.AuthService
import nl.greaper.bnplanner.service.OsuService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping
    fun login(@RequestBody token: String) = authService.login(token)

    @PostMapping("/refresh")
    fun refreshLogin(@RequestBody refreshToken: String) = authService.refresh(refreshToken)
}
