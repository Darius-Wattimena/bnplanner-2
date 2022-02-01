package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.LegacyBeatmap
import nl.greaper.bnplanner.model.beatmap.NewBeatmap
import nl.greaper.bnplanner.service.BeatmapService
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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
    @RolesAllowed(RolePermission.VIEWER)
    fun findBeatmap(@PathVariable("id") id: String): Beatmap? {
        return service.findBeatmap(id)
    }

    @GetMapping("/count")
    @RolesAllowed(RolePermission.VIEWER)
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

    @GetMapping("/find")
    @RolesAllowed(RolePermission.VIEWER)
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

    @PostMapping("/add")
    @RolesAllowed(RolePermission.EDITOR)
    fun addBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestBody newBeatmap: NewBeatmap
    ) {
        service.addBeatmap(osuApiToken, newBeatmap)
    }

    @PostMapping("/update/{id}")
    @RolesAllowed(RolePermission.EDITOR)
    fun updateBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestBody gamemodes: Map<Gamemode, BeatmapGamemode>,
        @PathVariable("id") id: String
    ) {
        service.updateBeatmap(id, gamemodes)
    }

    @DeleteMapping("/delete/{id}")
    @RolesAllowed(RolePermission.ADMIN)
    fun deleteBeatmap(@PathVariable("id") id: String) {
        service.deleteBeatmap(id)
    }
}