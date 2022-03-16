package nl.greaper.bnplanner.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("mongodb")
data class MongoConfig(
    val db: String,
    val username: String,
    val password: String,
    val authDb: String,
    val host: String,
    val port: Int
)

@ConstructorBinding
@ConfigurationProperties("osu")
data class OsuConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String
)

@ConstructorBinding
@ConfigurationProperties("cors")
data class CorsConfig(
    val uris: String,
    val methods: String,
    val headers: String
)

@ConstructorBinding
@ConfigurationProperties("discord")
data class DiscordConfig(
    val webhook: String,
    val webhookPublic: String
)
