package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.UserRecalculate
import nl.greaper.bnplanner.model.osu.AuthToken
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Component

@Component
class OsuTokenDataSource(private val database: MongoDatabase) : BaseDataSource<AuthToken>() {
    override fun initCollection(): MongoCollection<AuthToken> {
        return database.getCollection<AuthToken>("authToken")
    }
}
