package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Component

@Component
class UserDataSource(private val database: MongoDatabase) : BaseDataSource<User>() {
    override fun initCollection(): MongoCollection<User> {
        return database.getCollection<User>("users")
    }

    // Keep users in memory to speed up requests
    private val users = list().toMutableList()
    private val usersByOsuId = users.associateBy { it.osuId }.toMutableMap()
    private val usersByGamemode = users
        .flatMap { user -> user.gamemodes.map { it.gamemode to user } }
        .groupBy({ (gamemode, _) -> gamemode }, { (_, user) -> user })
        .toMutableMap()

    private fun removeInMemoryUser(user: User) {
        users.remove(user)
        usersByOsuId.remove(user.username)

        for (userGamemode in user.gamemodes) {
            val newUsers = usersByGamemode[userGamemode.gamemode]?.toMutableList() ?: continue
            newUsers.remove(user)
            usersByGamemode[userGamemode.gamemode] = newUsers
        }
    }

    private fun addInMemoryUser(user: User) {
        users.add(user)
        usersByOsuId[user.osuId] = user

        for (userGamemode in user.gamemodes) {
            val newUsers = usersByGamemode[userGamemode.gamemode]?.toMutableList() ?: continue
            newUsers.add(user)
            usersByGamemode[userGamemode.gamemode] = newUsers
        }
    }

    fun searchUser(username: String?, gamemodes: Set<Gamemode>, roles: Set<Role>): Set<User> {
        val gamemodeUsers = if (gamemodes.isNotEmpty()) {
            gamemodes.map { gamemode ->
                usersByGamemode[gamemode] ?: emptyList()
            }.flatten().toSet()
        } else {
            usersByGamemode.flatMap { it.value }.toSet()
        }

        val usernameFilteredUsers = username?.let { gamemodeUsers.filter { user -> user.username.contains(username, true) } } ?: gamemodeUsers

        return if (roles.isNotEmpty()) {
            roles.map { role ->
                usernameFilteredUsers.filter {
                    it.gamemodes.any { userGamemode -> userGamemode.role == role }
                }
            }.flatten().toSet()
        } else {
            usernameFilteredUsers.toSet()
        }
    }

    fun findUser(osuId: String): User? {
        return usersByOsuId[osuId]
    }

    fun saveUser(user: User) {
        collection.save(user)
        addInMemoryUser(user)
    }
}
