package nl.greaper.bnplanner.client

import com.fasterxml.jackson.databind.ObjectMapper
import nl.greaper.bnplanner.config.DiscordConfig
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedMessage
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.discord.Message
import nl.greaper.bnplanner.model.discord.getValue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Component
class DiscordWebhookClient(
    private val config: DiscordConfig,
    private val objectMapper: ObjectMapper
) {

    private val rest = RestTemplate()
    private val headers = HttpHeaders()

    fun sendBeatmapUpdate(
        description: String,
        color: EmbedColor,
        beatmapId: String,
        editor: User?,
        confidential: Boolean,
        gamemodes: List<Gamemode>
    ) {
        return send(
            description = description,
            color = color,
            thumbnail = EmbedThumbnail("https://b.ppy.sh/thumb/${beatmapId}l.jpg"),
            footer = EmbedFooter(editor?.username ?: "", "https://a.ppy.sh/${editor?.osuId}"),
            confidential = confidential,
            gamemodes = gamemodes
        )
    }

    fun send(
        description: String,
        color: EmbedColor,
        thumbnail: EmbedThumbnail,
        footer: EmbedFooter,
        confidential: Boolean,
        gamemodes: List<Gamemode>
    ) {
        return send(
            EmbedMessage(
                description = description,
                // timestamp = Instant.now().toString(),
                color = color.getValue(),
                thumbnail = thumbnail,
                footer = footer
            ),
            confidential,
            gamemodes
        )
    }

    private fun send(
        embedMessage: EmbedMessage,
        confidential: Boolean,
        gamemodes: List<Gamemode>
    ) {
        headers.contentType = MediaType.APPLICATION_JSON
        // Private discord server with all messages
        val webhookUrl = config.webhook

        val body = objectMapper.writeValueAsString(Message(listOf(embedMessage)))
        val request = HttpEntity(body, headers)

        if (!confidential) {
            // Public catch mapping hub feed with only the most informative messages
            val moddingServerWebhookUrl = config.webhookPublic

            if (moddingServerWebhookUrl.isNotBlank() && gamemodes.contains(Gamemode.fruits)) {
                rest.postForEntity<String>(moddingServerWebhookUrl, request)
            }
        }

        if (webhookUrl.isNotBlank()) {
            rest.postForEntity<String>(webhookUrl, request)
        }
    }
}
