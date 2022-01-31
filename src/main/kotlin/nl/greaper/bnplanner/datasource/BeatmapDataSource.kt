package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.Beatmap
import org.bson.conversions.Bson
import org.litote.kmongo.and
import org.litote.kmongo.ascending
import org.litote.kmongo.descending
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Component

@Component
class BeatmapDataSource(private val database: MongoDatabase): BaseDataSource<Beatmap>() {
    override fun initCollection(): MongoCollection<Beatmap> {
        return database.getCollection<Beatmap>("beatmaps").also {
            it.ensureIndex(Beatmap::artist)
            it.ensureIndex(Beatmap::title)
            it.ensureIndex(Beatmap::mapperId)
            it.ensureIndex(Beatmap::status)
            it.ensureIndex(Beatmap::dateUpdated)
        }

    }

    fun findAll(
        filter: Bson,
        from: Int,
        to: Int,
    ): List<Beatmap> {
        val findQuery = collection.find(filter)
        findQuery.limit(to - from)
        findQuery.skip(from)
        findQuery.sort(and(ascending(Beatmap::status), descending(Beatmap::dateUpdated)))

        return findQuery.toMutableList()
    }
}