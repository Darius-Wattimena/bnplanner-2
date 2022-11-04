package nl.greaper.bnplanner.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import nl.greaper.bnplanner.config.OsuConfig
import nl.greaper.bnplanner.model.osu.AuthToken
import nl.greaper.bnplanner.model.osu.BeatmapSet
import nl.greaper.bnplanner.model.osu.Me
import nl.greaper.bnplanner.model.osu.OsuOAuth
import nl.greaper.bnplanner.model.osu.RefreshToken
import nl.greaper.bnplanner.util.shouldSkipUser
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Component
class OsuHttpClient(
    val config: OsuConfig,
    val objectMapper: ObjectMapper
) {
    val log = KotlinLogging.logger { }

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
    fun getToken(code: String): ResponseEntity<AuthToken>? {
        val osuOAuth = OsuOAuth(
            client_id = config.clientId.toInt(),
            client_secret = config.clientSecret,
            code = code,
            grant_type = "authorization_code",
            redirect_uri = config.redirectUri
        )

        val body = objectMapper.writeValueAsString(osuOAuth)
        val request = HttpEntity(body, authHeaders)

        runCatching {
            return authRest.postForEntity(tokenUri, request)
        }

        return null
    }

    fun refreshToken(refreshToken: String): ResponseEntity<AuthToken>? {
        val preparedRefreshToken = RefreshToken(
            grant_type = "refresh_token",
            client_id = config.clientId.toInt(),
            client_secret = config.clientSecret,
            refresh_token = refreshToken,
        )

        val body = objectMapper.writeValueAsString(preparedRefreshToken)
        val request = HttpEntity(body, authHeaders)

        return try {
            authRest.postForEntity(tokenUri, request)
        } catch (ex: HttpClientErrorException) {
            if (ex.statusCode == HttpStatus.UNAUTHORIZED) {
                log.info { "[UNAUTHORIZED] User trying to login via their refresh token, but it isn't valid" }
                null
            } else {
                throw ex
            }
        }
    }

    fun get(uri: String, osuApiToken: String, includeBearer: Boolean): ResponseEntity<String> {
        return request(uri, HttpMethod.GET, osuApiToken, includeBearer = includeBearer)
    }

    fun findBeatmapWithId(osuApiToken: String, osuId: String, includeBearer: Boolean = true): BeatmapSet? {
        return try {
            val response = get("/beatmapsets/$osuId", osuApiToken, includeBearer)
            return response.body?.let { objectMapper.readValue<BeatmapSet>(it) }
        } catch (ex: Exception) {
            log.error(ex) { "Unable to get a beatmap from the osu api, using id $osuId" }
            null
        }
    }

    fun findUserWithId(osuApiToken: String, osuId: String, includeBearer: Boolean = true): Me? {
        if (shouldSkipUser(osuId)) {
            return null
        }

        return try {
            val response = get("/users/$osuId?key=id", osuApiToken, includeBearer)
            return response.body?.let { objectMapper.readValue<Me>(it) }
        } catch (ex: Exception) {
            log.error(ex) { "Unable to get a user from the osu api, using id $osuId" }
            null
        }
    }

    private fun request(uri: String, method: HttpMethod, authToken: String, body: String = "", includeBearer: Boolean): ResponseEntity<String> {
        if (includeBearer) {
            headers.setBearerAuth(authToken)
        } else {
            headers.set(HttpHeaders.AUTHORIZATION, authToken)
        }

        val request = if (body == "") {
            HttpEntity(headers)
        } else {
            HttpEntity(body, headers)
        }
        val requestUri = "https://osu.ppy.sh/api/v2$uri"

        log.info { "$method ==> $requestUri" }

        return rest.exchange(requestUri, method, request, String::class.java)
    }
}
