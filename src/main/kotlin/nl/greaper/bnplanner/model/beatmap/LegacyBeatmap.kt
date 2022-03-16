package nl.greaper.bnplanner.model.beatmap

import org.bson.codecs.pojo.annotations.BsonId

data class LegacyBeatmap(
    @BsonId
    val _id: Long,
    val artist: String,
    val title: String,
    val note: String,
    val mapper: String,
    var mapperId: Long,
    val status: Long,
    val nominators: List<Long>,
    val dateAdded: Long = 0,
    val dateUpdated: Long = 0,
    val dateRanked: Long = 0,
    val nominatedByBNOne: Boolean = false,
    val nominatedByBNTwo: Boolean = false,
    val unfinished: Boolean = false
)
