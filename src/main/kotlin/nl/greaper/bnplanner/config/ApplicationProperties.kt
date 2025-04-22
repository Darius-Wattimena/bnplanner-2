package nl.greaper.bnplanner.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mongodb")
data class MongoProperties(
    val db: String,
    val username: String,
    val password: String,
    val authDb: String,
    val host: String,
    val port: Int
)

@ConfigurationProperties("osu")
data class OsuProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String
)

@ConfigurationProperties("cors")
data class CorsProperties(
    val uris: String,
    val methods: String,
    val headers: String
)

@ConfigurationProperties("discord")
data class DiscordProperties(
    val token: String,
    val webhook: String,
    val webhookPublic: String
)

@ConfigurationProperties("aiess")
data class AiessProperties(
    val token: String
)
