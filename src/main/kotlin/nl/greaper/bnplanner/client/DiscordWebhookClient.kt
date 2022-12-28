package nl.greaper.bnplanner.client

import mu.KotlinLogging
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import nl.greaper.bnplanner.datasource.DiscordEventListenerDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedMessage
import nl.greaper.bnplanner.model.discord.EmbedThumbnail
import nl.greaper.bnplanner.model.discord.getValue
import org.springframework.stereotype.Component

@Component
class DiscordWebhookClient(
    private val dataSource: DiscordEventListenerDataSource,
    private val jda: JDA
) {

    val log = KotlinLogging.logger { }

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
        val messageEmbed = EmbedBuilder()
            .setDescription(embedMessage.description)
            .setColor(embedMessage.color)
            .setFooter(embedMessage.footer.text, embedMessage.footer.icon_url)
            .setThumbnail(embedMessage.thumbnail.url)
            .build()

        val listeners = dataSource.getListeners()

        for (listener in listeners) {
            val isCorrectGamemode = listener.gamemode != null && gamemodes.contains(listener.gamemode)
            val isPublicMessage = ((gamemodes.isEmpty() || (listener.gamemode == null || isCorrectGamemode)) && !confidential)

            // Only send a message when relevant for the listener
            if ((confidential && listener.confidential) || (!listener.confidential && isPublicMessage)) {
                try {
                    val textChannel = jda.getTextChannelById(listener.channelId)

                    if (textChannel != null) {
                        textChannel.sendMessageEmbeds(messageEmbed).queue()
                    } else {
                        // Discord channel has probably been deleted or bot has been removed, removing from listeners
                        dataSource.remove(listener)
                    }
                } catch (ex: Throwable) {
                    log.error(ex) { "Could not deliver discord message to server" }
                }
            }
        }
    }
}
