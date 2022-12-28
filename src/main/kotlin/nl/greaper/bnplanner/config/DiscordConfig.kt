package nl.greaper.bnplanner.config

import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import nl.greaper.bnplanner.datasource.DiscordEventListenerDataSource
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.discord.DiscordEventListener
import nl.greaper.bnplanner.util.toReadableName
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscordConfig(private val dataSource: DiscordEventListenerDataSource) {

    @Bean
    fun jda(properties: DiscordProperties): JDA {
        return JDABuilder.createDefault(properties.token)
            .addEventListeners(DiscordBotHandler(dataSource))
            .build()
    }

    class DiscordBotHandler(private val dataSource: DiscordEventListenerDataSource) : ListenerAdapter() {
        val log = KotlinLogging.logger { }

        override fun onSlashCommand(event: SlashCommandEvent) {
            // Ignore all events not from a Discord guild
            if (!event.isFromGuild) {
                return
            }

            if (event.name == "planner" && event.subcommandName == "register") {
                log.info { "Handling discord message" }
                val gamemodeValue = event.getOption("gamemode")?.asString
                val gamemode = if (gamemodeValue != null) {
                    Gamemode.valueOf(gamemodeValue)
                } else {
                    null
                }

                val replyMessage = StringBuilder("Nomination planner events will be published in this channel")
                    .let { builder ->
                        if (gamemode != null) {
                            builder.append(" for gamemode ${gamemode.toReadableName()}")
                        } else {
                            builder
                        }
                    }
                    .toString()

                event.reply(replyMessage).queue()

                val guildId = event.guild!!.id
                val channelId = event.channel.id

                val existingListener = dataSource.findByGuildId(guildId)
                    .find { it.channelId == channelId }

                val newListener = DiscordEventListener(
                    guildId = guildId,
                    channelId = channelId,
                    gamemode = gamemode
                )

                // Listener doesn't exist yet, create a new one
                if (existingListener == null) {
                    dataSource.create(newListener)
                } else {
                    // Listener is already known for this channel, update it with the new configuration
                    dataSource.replace(existingListener, newListener)
                }
            }
        }

        private fun getCommand(): CommandData {
            val gamemodeOption = OptionData(OptionType.STRING, "gamemode", "The osu! gamemode the event is for")
                .addChoices(Gamemode.values().map { Choice(it.toReadableName(), it.name) })

            val registerCommand = SubcommandData("register", "Register a listener of bnplanner events")
                .addOptions(gamemodeOption)

            return CommandData("planner", "All nomination planner slash commands")
                .addSubcommands(listOf(registerCommand))
        }

        override fun onGuildReady(event: GuildReadyEvent) {
            // FIXME remove, currently for testing
            // ID of Nomination Planner servers
            if (event.guild.id == "761157153884864532") {
                event.guild.updateCommands()
                    .addCommands(getCommand())
                    .queue()
            }
        }
    }
}
