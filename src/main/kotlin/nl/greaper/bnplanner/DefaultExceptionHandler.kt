package nl.greaper.bnplanner

import mu.KotlinLogging
import nl.greaper.bnplanner.client.DiscordClient
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class DefaultExceptionHandler(
    private val discordClient: DiscordClient
) : ResponseEntityExceptionHandler() {
    private val log = KotlinLogging.logger { }

    @ExceptionHandler
    fun exception(ex: Exception): ResponseEntity<String> {
        discordClient.send(
            "**ERROR:**\n" +
                "```\n${ex.stackTraceToString().take(1600)}\n```",
            color = EmbedColor.RED,
            thumbnail = EmbedThumbnail(""),
            footer = EmbedFooter(""),
            confidential = true,
            gamemodes = emptyList()
        )

        log.error(ex) { "Unhandled exception occurred" }

        return ResponseEntity("Unable to handle request", HttpStatus.BAD_REQUEST)
    }
}
