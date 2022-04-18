package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.osu.AuthToken
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Component

@Component
class OsuTokenDataSource(private val database: MongoDatabase) : BaseDataSource<AuthToken>() {
    override fun initCollection(): MongoCollection<AuthToken> {
        return database.getCollection<AuthToken>("authToken")
    }
}
