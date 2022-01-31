package nl.greaper.bnplanner.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import nl.greaper.bnplanner.config.OsuConfig
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.OsuOAuth
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate


@Component
class OsuHttpClient(
    val config: OsuConfig,
    val objectMapper: ObjectMapper
) {
    private val rest = RestTemplate()
    private val authRest = RestTemplate()
    private val headers = HttpHeaders()
    private val authHeaders = HttpHeaders()
    private val tokenUri = "https://osu.ppy.sh/oauth/token"

    init {
        authHeaders.contentType = MediaType.APPLICATION_JSON
        authHeaders.accept = listOf(MediaType.APPLICATION_JSON)
    }

    /**
     * Get a token from the osu server
     */
    fun getToken(code: String): ResponseEntity<String> {
        val osuOAuth = OsuOAuth(
                config.clientId.toInt(),
                config.clientSecret,
                code,
                "authorization_code",
                config.redirectUri
        )

        val body = objectMapper.writeValueAsString(osuOAuth)
        val request = HttpEntity(body, authHeaders)

        return authRest.postForEntity(tokenUri, request, String::class.java)
    }

    fun get(uri: String, osuApiToken: String) : ResponseEntity<String> {
        return request(uri, HttpMethod.GET, osuApiToken)
    }

    fun findUserWithId(osuApiToken: String, osuId: String): Me? {
        return try {
            val response = get("/users/$osuId", osuApiToken)
            return response.body?.let { objectMapper.readValue<Me>(it) }
        } catch (ex: Exception) {
            null
        }
    }

    private fun request(uri: String, method: HttpMethod, authToken: String, body: String = "") : ResponseEntity<String> {
        headers.remove("Authorization")
        headers.set("Authorization", "Bearer $authToken")

        val request = if (body == "") {
            HttpEntity(headers)
        } else {
            HttpEntity(body, headers)
        }
        return rest.exchange("https://osu.ppy.sh/api/v2$uri", method, request, String::class.java)
    }
}