package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.User
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Component

@Component
class UserDataSource(private val database: MongoDatabase): BaseDataSource<User>() {
    override fun initCollection(): MongoCollection<User> {
        return database.getCollection<User>("users")
    }

    // Keep users in memory to speed up requests
    private val users by lazy {
        list().associateBy { it.osuId }.toMutableMap()
    }

    fun findUser(osuId: String): User? {
        return users[osuId]
    }

    fun saveUser(user: User) {
        collection.save(user)
        users[user.osuId] = user
    }
}