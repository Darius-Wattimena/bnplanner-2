package nl.greaper.bnplanner.client

import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.discord.EmbedColor
import nl.greaper.bnplanner.model.discord.EmbedFooter
import nl.greaper.bnplanner.model.discord.EmbedThumbnail

interface DiscordClient {
    fun sendBeatmapUpdate(
        description: String,
        color: EmbedColor,
        beatmapId: String,
        editor: User?,
        confidential: Boolean,
        gamemodes: List<Gamemode>
    )

    fun send(
        description: String,
        color: EmbedColor,
        thumbnail: EmbedThumbnail,
        footer: EmbedFooter,
        confidential: Boolean,
        gamemodes: List<Gamemode>
    )
}
