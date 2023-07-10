package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.security.RolesAllowed

@RestController
@RequestMapping("/v2/user")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/search")
    @RolesAllowed(RolePermission.VIEWER)
    fun searchUser(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false) gamemodes: Set<Gamemode>?,
        @RequestParam(required = false) roles: Set<Role>?,
    ): List<User> {
        return userService.searchUser(username, gamemodes, roles)
    }
}
