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
    // Keep users in memory to speed up requests
    final val usersByOsuId: MutableMap<String, User> = mutableMapOf()
    final val usersByGamemode: MutableMap<Gamemode, Set<String>> = mutableMapOf()

    init {
        Gamemode.values().forEach { gamemode ->
            usersByGamemode[gamemode] = emptySet()
        }

        list().forEach {
            addInMemoryUser(it)
        }
    }

    override fun initCollection(): MongoCollection<User> {
        return database.getCollection<User>("users")
    }

    fun removeInMemoryUser(user: User) {
        usersByOsuId.remove(user.osuId)

        // Loop over all gamemodes as we don't know anymore what gamemode each user was part of
        usersByGamemode.forEach { (gamemode, usersIds) ->
            if (usersIds.contains(user.osuId)) {
                val userIdsWithoutDeleted = usersIds - user.osuId
                usersByGamemode[gamemode] = userIdsWithoutDeleted
            }
        }
    }

    fun addInMemoryUser(user: User) {
        usersByOsuId[user.osuId] = user

        val userGamemodes = user.gamemodes.map { it.gamemode }

        Gamemode.values().forEach { gamemode ->
            val gamemodeUsers = usersByGamemode[gamemode]?.toMutableSet() ?: mutableSetOf()
            if (gamemode in userGamemodes) {
                // Add the user to the gamemode
                gamemodeUsers.add(user.osuId)
            } else {
                // Make sure to remove the user from the gamemode if it previously was
                gamemodeUsers.remove(user.osuId)
            }

            // Replace all users of our map
            usersByGamemode[gamemode] = gamemodeUsers
        }
    }

    fun searchUser(username: String?, gamemodes: Set<Gamemode>, roles: Set<Role>): Set<User> {
        val gamemodeUserIds = if (gamemodes.isNotEmpty()) {
            gamemodes.map { gamemode ->
                usersByGamemode[gamemode] ?: emptyList()
            }.flatten().toSet()
        } else {
            usersByGamemode.flatMap { it.value }.toSet()
        }

        val gamemodeUsers = usersByOsuId.filterKeys { it in gamemodeUserIds }.values
        val usernameFilteredUsers = username?.let { gamemodeUsers.filter { user -> user.username.contains(username, true) } } ?: gamemodeUsers

        return if (roles.isNotEmpty()) {
            roles.map { role ->
                usernameFilteredUsers.filter {
                    it.gamemodes.any { userGamemode -> userGamemode.role == role }
                }
            }.flatten().toSet()
        } else {
            // Don't show mappers when searching, except when explicitly providing them
            usernameFilteredUsers.filter { user -> user.gamemodes.all { gamemode -> gamemode.role != Role.Mapper } }
                .toSet()
        }
    }

    fun findUser(osuId: String): User? {
        return usersByOsuId[osuId]
    }

    fun saveUser(user: User) {
        collection.save(user)
        addInMemoryUser(user)
    }

    fun deleteUser(user: User) {
        removeInMemoryUser(user)
        deleteById(user.osuId)
    }
}
