package nl.greaper.bnplanner.controller

import nl.greaper.bnplanner.auth.RolePermission
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.PageLimit
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import nl.greaper.bnplanner.model.beatmap.BeatmapPage
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.ExposedBeatmap
import nl.greaper.bnplanner.model.beatmap.LegacyBeatmap
import nl.greaper.bnplanner.model.beatmap.NewBeatmap
import nl.greaper.bnplanner.service.BeatmapService
import nl.greaper.bnplanner.service.FixService
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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
    val service: BeatmapService,
    val fixService: FixService
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
        @RequestParam(required = false) nominators: Set<String>?,
        @RequestParam(required = false) gamemodes: Set<Gamemode>?,
        @RequestParam(required = false) missingNominator: Set<Gamemode>?
    ): Int {
        return service.countBeatmaps(
            artist = artist,
            title = title,
            mapper = mapper,
            status = status ?: emptySet(),
            nominators = nominators ?: emptySet(),
            page = page,
            gamemodes = gamemodes ?: emptySet(),
            missingNominator = missingNominator ?: emptySet()
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
        @RequestParam(required = false) nominators: Set<String>?,
        @RequestParam(required = false) gamemodes: Set<Gamemode>?,
        @RequestParam(required = false) missingNominator: Set<Gamemode>?
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
            to = to,
            gamemodes = gamemodes ?: emptySet(),
            missingNominator = missingNominator ?: emptySet()
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
        @RequestParam(required = false) nominators: Set<String>?,
        @RequestParam(required = false) gamemodes: Set<Gamemode>?,
        @RequestParam(required = false) missingNominator: Set<Gamemode>?
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
            pageLimit = pageLimit,
            gamemodes = gamemodes ?: emptySet(),
            missingNominator = missingNominator ?: emptySet()
        )
    }

    @PostMapping("/add")
    @RolesAllowed(RolePermission.EDITOR)
    fun addBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestBody newBeatmap: NewBeatmap
    ): ExposedBeatmap? {
        return service.addBeatmap(osuApiToken, newBeatmap)
    }

    @PatchMapping("/{id}/nominators")
    @RolesAllowed(RolePermission.EDITOR)
    fun updateNominators(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @PathVariable("id") id: String,
        @RequestBody updatedBeatmapGamemodes: List<BeatmapGamemode>
    ): ExposedBeatmap? {
        return service.updateBeatmapNominators(osuApiToken, id, updatedBeatmapGamemodes)
    }

    @PatchMapping("/{id}/{mode}/nominator")
    @RolesAllowed(RolePermission.EDITOR)
    fun updateSingleNominator(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam old: String,
        @RequestParam new: String,
        @PathVariable("id") id: String,
        @PathVariable("mode") mode: Gamemode
    ): ExposedBeatmap? {
        return service.updateBeatmapNominator(osuApiToken, id, mode, old, new)
    }

    @PatchMapping("/{id}/status")
    @RolesAllowed(RolePermission.ADMIN)
    fun updateStatus(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam new: BeatmapStatus,
        @PathVariable("id") id: String,
    ): Boolean {
        return service.updateBeatmapStatus(osuApiToken, id, new)
    }

    @PatchMapping("/{id}/note", consumes = ["text/plain"])
    @RolesAllowed(RolePermission.EDITOR)
    fun updateNote(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestBody body: String,
        @PathVariable("id") id: String,
    ): Boolean {
        return service.updateBeatmapNote(osuApiToken, id, body)
    }

    @DeleteMapping("/{id}/delete")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun deleteBeatmap(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @PathVariable("id") id: String
    ) {
        service.deleteBeatmap(osuApiToken, id)
    }

    @GetMapping("/fix")
    @RolesAllowed(RolePermission.DEVELOPER)
    fun fixBeatmapsByStatus(
        @RequestHeader(HttpHeaders.AUTHORIZATION) osuApiToken: String,
        @RequestParam status: BeatmapStatus
    ) {
        fixService.syncBeatmaps(osuApiToken, status)
    }
}
