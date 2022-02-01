package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Beatmap
import nl.greaper.bnplanner.model.BeatmapPage
import nl.greaper.bnplanner.model.BeatmapStatus
import nl.greaper.bnplanner.model.LegacyBeatmap
import nl.greaper.bnplanner.service.BeatmapService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.security.RolesAllowed

@RestController
@RequestMapping("/v2/beatmaps")
class BeatmapController(
    val service: BeatmapService
) {
    @PostMapping("/import")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun importBeatmaps(@RequestBody body: List<LegacyBeatmap>) {
        service.importLegacyBeatmaps(body)
    }
    
    @GetMapping("/{id}")
    fun findBeatmap(@PathVariable("id") id: String): Beatmap? {
        return service.findBeatmap(id)
    }

    @GetMapping("/countBeatmaps")
    fun countBeatmaps(
        @RequestParam page: BeatmapPage,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) mapper: String?,
        @RequestParam(required = false) status: Set<BeatmapStatus> = emptySet(),
        @RequestParam(required = false) nominators: Set<String> = emptySet()
    ): Int {
        return service.countBeatmaps(artist, title, mapper, status, nominators, page)
    }

    @GetMapping("/findBeatmaps")
    fun findBeatmaps(
        @RequestParam page: BeatmapPage,
        @RequestParam from: Int,
        @RequestParam to: Int,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) mapper: String?,
        @RequestParam(required = false) status: Set<BeatmapStatus> = emptySet(),
        @RequestParam(required = false) nominators: Set<String> = emptySet()
    ): List<Beatmap> {
        return service.findBeatmaps(artist, title, mapper, status, nominators, page, from, to)
    }
}