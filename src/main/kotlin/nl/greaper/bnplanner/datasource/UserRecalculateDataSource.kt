package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.UserRecalculate
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Component

@Component
class UserRecalculateDataSource(private val database: MongoDatabase) : BaseDataSource<UserRecalculate>() {
    override fun initCollection(): MongoCollection<UserRecalculate> {
        return database.getCollection<UserRecalculate>("usersRecalculate")
    }
}
