package nl.greaper.bnplanner.model.discord

import nl.greaper.bnplanner.model.Gamemode

data class DiscordEventListener(
    /**
     * The Guild ID of the server in Discord.
     */
    val guildId: String,

    /**
     * The ID of the channel where the event is posted.
     */
    val channelId: String,

    /**
     * When provided filter said events on the specified gamemode.
     */
    val gamemode: Gamemode?,

    /**
     * Should only be true for the testing discord server.
     */
    val confidential: Boolean = false
)
