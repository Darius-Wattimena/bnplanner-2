package nl.greaper.bnplanner.model

import org.bson.codecs.pojo.annotations.BsonId

data class UserRecalculate(
    @BsonId
    val osuId: String
)
