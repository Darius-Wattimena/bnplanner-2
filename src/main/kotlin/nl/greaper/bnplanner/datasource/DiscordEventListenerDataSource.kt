package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import nl.greaper.bnplanner.model.discord.DiscordEventListener
import org.litote.kmongo.and
import org.litote.kmongo.deleteOne
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.springframework.stereotype.Component

@Component
class DiscordEventListenerDataSource(private val database: MongoDatabase) : BaseDataSource<DiscordEventListener>() {
    override fun initCollection(): MongoCollection<DiscordEventListener> {
        return database.getCollection<DiscordEventListener>("discord")
    }

    private val listeners = this.list().toMutableList()

    private fun addInMemoryListener(listener: DiscordEventListener) {
        listeners.add(listener)
    }

    private fun removeInMemoryListener(listener: DiscordEventListener) {
        listeners.remove(listener)
    }

    fun getListeners() = listeners.toList()

    fun findByGuildId(guildId: String): List<DiscordEventListener> {
        return listeners.filter {
            it.guildId == guildId
        }
    }

    fun create(newListener: DiscordEventListener): InsertOneResult {
        addInMemoryListener(newListener)
        return insertOne(newListener)
    }

    fun replace(oldListener: DiscordEventListener, newListener: DiscordEventListener): UpdateResult {
        removeInMemoryListener(oldListener)
        addInMemoryListener(newListener)

        return collection.replaceOne(
            and(
                DiscordEventListener::guildId eq oldListener.guildId,
                DiscordEventListener::channelId eq oldListener.channelId
            ),
            newListener
        )
    }

    fun remove(listener: DiscordEventListener): DeleteResult {
        removeInMemoryListener(listener)
        return collection.deleteOne(
            and(
                DiscordEventListener::guildId eq listener.guildId,
                DiscordEventListener::channelId eq listener.channelId
            )
        )
    }
}
