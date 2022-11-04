package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.model.UserContext
import nl.greaper.bnplanner.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping
    fun login(@RequestBody token: String): ResponseEntity<UserContext?> {
        runCatching {
            val result = authService.login(token)

            if (result != null) {
                return ResponseEntity.ok(result)
            }
        }

        return ResponseEntity.badRequest().body(null)
    }

    @PostMapping("/refresh")
    fun refreshLogin(@RequestBody refreshToken: String): ResponseEntity<UserContext?> {
        runCatching {
            val result = authService.refresh(refreshToken)

            if (result != null) {
                return ResponseEntity.ok(result)
            }
        }

        return ResponseEntity.badRequest().body(null)
    }
}
