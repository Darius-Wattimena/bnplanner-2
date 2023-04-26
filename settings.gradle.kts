rootProject.name = "bnplanner-backend"

buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 14
    }
}
