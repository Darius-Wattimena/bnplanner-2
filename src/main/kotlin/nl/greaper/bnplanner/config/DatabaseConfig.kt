package nl.greaper.bnplanner.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoClientSettings.getDefaultCodecRegistry
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.service.ClassMappingType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URLEncoder

@Configuration
class DatabaseConfig {
    val encoding = "UTF-8"

    @Bean
    fun mongoClient(config: MongoProperties): MongoClient {
        val uri = if (config.username.isNotBlank() && config.password.isNotBlank()) {
            val encodedUsername = URLEncoder.encode(config.username, encoding)
            val encodedPassword = URLEncoder.encode(config.password, encoding)
            val encodedAuthDb = URLEncoder.encode(config.authDb, encoding)
            "mongodb+srv://$encodedUsername:$encodedPassword@${config.host}/$encodedAuthDb"
        } else {
            "mongodb://${config.host}:${config.port}"
        }

        val settings = MongoClientSettings.builder()
            .codecRegistry(ClassMappingType.codecRegistry(getDefaultCodecRegistry()))
            .applyConnectionString(ConnectionString(uri))
            .build()
        return MongoClients.create(settings)
    }

    @Bean
    fun mongoDatabase(config: MongoProperties, client: MongoClient): MongoDatabase {
        return client.getDatabase(config.db)
    }
}
