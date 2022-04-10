package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.PageLimit
import nl.greaper.bnplanner.model.beatmap.Beatmap
import nl.greaper.bnplanner.model.beatmap.BeatmapGamemode
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.springframework.stereotype.Component

@Component
class BeatmapDataSource(private val database: MongoDatabase) : BaseDataSource<Beatmap>() {
    override fun initCollection(): MongoCollection<Beatmap> {
        return database.getCollection<Beatmap>("beatmaps").also {
            it.ensureIndex(
                Beatmap::status,
                Beatmap::dateUpdated,
                Beatmap::artist,
                Beatmap::title
            )
        }
    }

    fun findAll(
        filter: Bson,
        pageNumber: Int,
        pageLimit: PageLimit,
    ): List<Beatmap> {
        val numberLimit = when (pageLimit) {
            PageLimit.TEN -> 10
            PageLimit.TWENTY -> 20
            PageLimit.FIFTY -> 50
        }

        val findQuery = collection.find(filter)
        findQuery.sort("{ status: 1, dateUpdated: -1 }")
        findQuery.limit(numberLimit)
        return if (pageNumber <= 1) {
            findQuery.toList()
        } else {
            val skip = numberLimit * (pageNumber - 1)
            findQuery.skip(skip).toList()
        }
    }

    fun findAll(
        filter: Bson,
        from: Int,
        to: Int,
    ): List<Beatmap> {
        val findQuery = collection.find(filter)
        findQuery.sort("{ status: 1, dateUpdated: -1 }")
        findQuery.limit(to - from)
        if (from > 0) {
            findQuery.skip(from)
        }

        return findQuery.toMutableList()
    }

    fun update(updatedBeatmap: Beatmap) {
        collection.replaceOneById(
            updatedBeatmap.osuId,
            updatedBeatmap
        )
    }
}
