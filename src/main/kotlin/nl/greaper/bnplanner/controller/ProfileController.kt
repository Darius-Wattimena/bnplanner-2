package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.profile.ProfileStatisticsPairInfo
import nl.greaper.bnplanner.service.ProfileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.security.RolesAllowed

@RestController
@RequestMapping("/v2/profile")
class ProfileController(private val service: ProfileService) {
    @RolesAllowed(RolePermission.VIEWER)
    @GetMapping("/{id}/pairing")
    fun getPairing(
        @PathVariable("id") id: String,
    ): List<ProfileStatisticsPairInfo> {
        return service.getUserPairing(id)
    }
}
