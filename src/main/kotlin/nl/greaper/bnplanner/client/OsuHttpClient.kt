package nl.greaper.bnplanner.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import nl.greaper.bnplanner.config.OsuConfig
import nl.greaper.bnplanner.model.osu.AuthToken
import nl.greaper.bnplanner.model.osu.BeatmapSet
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.OsuOAuth
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity


@Component
class OsuHttpClient(
    val config: OsuConfig,
    val objectMapper: ObjectMapper
) {
    val log = KotlinLogging.logger {  }

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
    fun getToken(code: String): ResponseEntity<AuthToken> {
        val osuOAuth = OsuOAuth(
                config.clientId.toInt(),
                config.clientSecret,
                code,
                "authorization_code",
                config.redirectUri
        )

        val body = objectMapper.writeValueAsString(osuOAuth)
        val request = HttpEntity(body, authHeaders)

        return authRest.postForEntity(tokenUri, request)
    }

    fun get(uri: String, osuApiToken: String) : ResponseEntity<String> {
        return request(uri, HttpMethod.GET, osuApiToken)
    }

    fun findBeatmapWithId(osuApiToken: String, osuId: String): BeatmapSet? {
        return try {
            val response = get("/beatmapsets/$osuId", osuApiToken)
            return response.body?.let { objectMapper.readValue<BeatmapSet>(it) }
        } catch (ex: Exception) {
            log.error(ex) { "Unable to get a beatmap from the osu api, using id $osuId" }
            null
        }
    }

    fun findUserWithId(osuApiToken: String, osuId: String): Me? {
        return try {
            val response = get("/users/$osuId?key=id", osuApiToken)
            return response.body?.let { objectMapper.readValue<Me>(it) }
        } catch (ex: Exception) {
            log.error(ex) { "Unable to get a user from the osu api, using id $osuId" }
            null
        }
    }

    private fun request(uri: String, method: HttpMethod, authToken: String, body: String = "") : ResponseEntity<String> {
        headers.remove(HttpHeaders.AUTHORIZATION)
        headers.set(HttpHeaders.AUTHORIZATION, authToken)

        val request = if (body == "") {
            HttpEntity(headers)
        } else {
            HttpEntity(body, headers)
        }
        return rest.exchange("https://osu.ppy.sh/api/v2$uri", method, request, String::class.java)
    }
}