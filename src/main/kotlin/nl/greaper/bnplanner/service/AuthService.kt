package nl.greaper.bnplanner.service

import mu.KotlinLogging
import nl.greaper.bnplanner.LOGIN_FAILED_ICON
import nl.greaper.bnplanner.LOGIN_ICON
import nl.greaper.bnplanner.client.DiscordWebhookClient
import nl.greaper.bnplanner.model.UserContext
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val osuService: OsuService,
    private val discordClient: DiscordWebhookClient
) {
    private val log = KotlinLogging.logger { }

    fun login(token: String): UserContext? {
        try {
            val authToken = osuService.getToken(token)
            val context = osuService.getUserContextByToken(authToken)
            return context.also { logLogin(it) }
        } catch (exception: Throwable) {
            logLogin(null, exception = exception)
            log.error(exception) { "User login failed" }
        }

        return null
    }

    fun refresh(refreshToken: String): UserContext? {
        try {
            val parsedToken = refreshToken.dropLast(1) // Somehow frontend always sends a trailing '='
            val authToken = osuService.getAuthTokenByRefreshToken(parsedToken)
            return osuService.getUserContextByToken(authToken).also { logLogin(it) }
        } catch (exception: Throwable) {
            logLogin(
                context = null,
                refresh = true,
                exception = exception
            )
            log.error(exception) { "User token refresh failed" }
        }

        return null
    }

    fun logLogin(context: UserContext?, refresh: Boolean = false, exception: Throwable? = null) {
        val user = context?.user

        val loginMessagePart = if (refresh) {
            "refreshing user login"
        } else {
            "logging in"
        }

        val loginMessage = when {
            exception?.message != null -> {
                "**ERROR:** Unexpected error occurred while $loginMessagePart.\n" +
                        exception.message
            }
            context == null -> "**ERROR:** Could not set up context while $loginMessagePart."
            user == null -> "**ERROR:** Could not find user while $loginMessagePart."
            else -> "**LOGIN:** ${user.username} ${if (refresh) "refreshed login" else "logged in"}."
        }

        discordClient.send(
            description = "${if (loginMessage.startsWith("**ERROR")) { LOGIN_FAILED_ICON } else { LOGIN_ICON }} $loginMessage",
            color = if (exception != null) EmbedColor.RED else EmbedColor.GREEN,
            thumbnail = EmbedThumbnail(user?.osuId?.let { "https://a.ppy.sh/$it" } ?: ""),
            footer = EmbedFooter(user?.username ?: "", "https://a.ppy.sh/${user?.osuId}"),
            confidential = true
        )
    }
}