package nl.greaper.bnplanner

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import nl.greaper.bnplanner.client.DiscordClient
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import org.bson.conversions.Bson
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration

@TestConfiguration
@Import(Application::class)
@ContextConfiguration(initializers = [ConfigDataApplicationContextInitializer::class])
class DefaultTestConfiguration {
    @Bean
    @Primary
    fun mongoTestDatabase(): MongoDatabase = getMockMongoDB()

    @Bean
    @Primary
    fun discordTestClient(): DiscordClient = TestDiscordClient()

    class TestDiscordClient : DiscordClient {
        override fun sendBeatmapUpdate(
            description: String,
            color: EmbedColor,
            beatmapId: String,
            editor: User?,
            confidential: Boolean,
            gamemodes: List<Gamemode>
        ) {
        }

        override fun send(
            description: String,
            color: EmbedColor,
            thumbnail: EmbedThumbnail,
            footer: EmbedFooter,
            confidential: Boolean,
            gamemodes: List<Gamemode>
        ) {
        }
    }

    private fun <T> getMockIterable() = mock<FindIterable<T>> {
        val mockIterator = mock<MongoCursor<T>> {
            on { hasNext() } doReturn false
        }
        on { iterator() } doReturn mockIterator
    }

    fun getMockMongoDB(): MongoDatabase {
        return mock {
            val mockCollection = mock<MongoCollection<Any>> {
                val iterable = getMockIterable<Any>()
                on { createIndex(any<Bson>(), any()) } doReturn ""
                on { find() } doReturn iterable
                on { find(any<Bson>()) } doReturn iterable
            }
            on { getCollection(any(), any<Class<Any>>()) } doReturn mockCollection
            val iterable = getMockIterable<String>()
            on { listCollectionNames() } doReturn iterable
        }
    }
}