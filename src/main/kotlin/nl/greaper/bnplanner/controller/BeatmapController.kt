package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.PageLimit
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.ExposedBeatmap
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
@RequestMapping("/v2/beatmap")
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
    fun findBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @PathVariable("id") id: String
    ): ExposedBeatmap? {
        return service.findExposedBeatmap(osuApiToken, id)
    }

    @GetMapping("/count")
    @RolesAllowed(RolePermission.VIEWER)
    fun countBeatmaps(
        @RequestParam page: BeatmapPage,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) mapper: String?,
        @RequestParam(required = false) status: Set<BeatmapStatus>?,
        @RequestParam(required = false) nominators: Set<String>?
    ): Int {
        return service.countBeatmaps(
            artist = artist,
            title = title,
            mapper = mapper,
            status = status ?: emptySet(),
            nominators = nominators ?: emptySet(),
            page = page
        )
    }

    @GetMapping("/find")
    @RolesAllowed(RolePermission.VIEWER)
    fun findBeatmaps(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam page: BeatmapPage,
        @RequestParam from: Int,
        @RequestParam to: Int,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) mapper: String?,
        @RequestParam(required = false) status: Set<BeatmapStatus>?,
        @RequestParam(required = false) nominators: Set<String>?
    ): List<ExposedBeatmap> {
        return service.findBeatmaps(
            osuApiToken = osuApiToken,
            artist = artist,
            title = title,
            mapper = mapper,
            status = status ?: emptySet(),
            nominators = nominators ?: emptySet(),
            page = page,
            from = from,
            to = to
        )
    }

    @GetMapping("/find/table")
    @RolesAllowed(RolePermission.VIEWER)
    fun findBeatmapsTable(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam page: BeatmapPage,
        @RequestParam pageNumber: Int,
        @RequestParam pageLimit: PageLimit,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) mapper: String?,
        @RequestParam(required = false) status: Set<BeatmapStatus>?,
        @RequestParam(required = false) nominators: Set<String>?
    ): List<ExposedBeatmap> {
        return service.findBeatmaps(
            osuApiToken = osuApiToken,
            artist = artist,
            title = title,
            mapper = mapper,
            status = status ?: emptySet(),
            nominators = nominators ?: emptySet(),
            page = page,
            pageNumber = pageNumber,
            pageLimit = pageLimit
        )
    }

    @PostMapping("/add")
    @RolesAllowed(RolePermission.EDITOR)
    fun addBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestBody newBeatmap: NewBeatmap
    ) {
        service.addBeatmap(osuApiToken, newBeatmap)
    }

    @GetMapping("/{id}/{mode}/update")
    @RolesAllowed(RolePermission.EDITOR)
    fun updateBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam old: String,
        @RequestParam new: String,
        @PathVariable("id") id: String,
        @PathVariable("mode") mode: Gamemode
    ): ExposedBeatmap? {
        return service.updateBeatmap(osuApiToken, id, mode, old, new)
    }

    @DeleteMapping("/delete/{id}")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun deleteBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @PathVariable("id") id: String
    ) {
        service.deleteBeatmap(osuApiToken, id)
    }
}
