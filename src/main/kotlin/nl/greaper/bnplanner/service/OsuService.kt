package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.client.OsuHttpClient
import nl.greaper.bnplanner.datasource.OsuTokenDataSource
import nl.greaper.bnplanner.model.UserContext
import nl.greaper.bnplanner.model.osu.AuthToken
import nl.greaper.bnplanner.util.getHighestRoleForUser
import nl.greaper.bnplanner.util.parseJwtToken
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class OsuService(
    private val client: OsuHttpClient,
    private val userService: UserService,
    private val osuTokenDataSource: OsuTokenDataSource
) {
    private val log = KotlinLogging.logger { }

    fun getToken(code: String): AuthToken {
        val parsedToken = code.dropLast(1) // Somehow frontend always sends a trailing '='

        require(parsedToken.isNotBlank()) { "Provided code is blank" }

        val response = client.getToken(parsedToken)
        val body = response.body

        check(response.statusCode.is2xxSuccessful && body != null) { "Couldn't get token from osu" }

        return body
    }

    fun getAuthTokenByRefreshToken(refreshToken: String): AuthToken? {
        val response = client.refreshToken(refreshToken)
        val body = response?.body

        if (response != null) {
            check(response.statusCode.is2xxSuccessful && body != null) { "Couldn't get token from osu" }
        }

        return body
    }

    fun getValidUpdaterToken(token: String, refreshToken: String): String? {
        val claims = parseJwtToken(token)
        val exp = (claims?.get("exp") as? Double)?.toLong()

        if (exp != null) {
            val actualExpire = Instant.ofEpochSecond(exp).minus(5, ChronoUnit.MINUTES)
            val isExpired = actualExpire.isBefore(Instant.now())

            if (isExpired) {
                val refreshedApiToken = getAuthTokenByRefreshToken(refreshToken)
                osuTokenDataSource.deleteMany()

                if (refreshedApiToken != null) {
                    osuTokenDataSource.insertOne(refreshedApiToken)

                    return refreshedApiToken.access_token
                }

                return null
            } else {
                return token
            }
        } else if (claims == null) {
            val refreshedApiToken = getAuthTokenByRefreshToken(refreshToken)
            osuTokenDataSource.deleteMany()
            if (refreshedApiToken != null) {
                osuTokenDataSource.insertOne(refreshedApiToken)

                return refreshedApiToken.access_token
            }

            return null
        }

        return null
    }

    fun getUserContextByToken(token: AuthToken): UserContext? {
        val tokenExpires = Instant.now().plusSeconds(token.expires_in.toLong())
            .minus(5, ChronoUnit.MINUTES)

        val claims = parseJwtToken(token.access_token) ?: return null

        val osuId = claims.subject
        val user = userService.findUserById(token.access_token, osuId)

        return UserContext(user, token.access_token, token.refresh_token, tokenExpires.toEpochMilli(), getHighestRoleForUser(user))
    }
}
