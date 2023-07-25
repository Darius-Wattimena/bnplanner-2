package nl.greaper.bnplanner.config

import mu.KotlinLogging
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.awt.Color

@Configuration
@ConditionalOnProperty(prefix = "discord", name = ["enabled"], havingValue = "true")
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

            if (event.name == "planner") {
                log.info { "Handling planner discord message. (guild = ${event.guild?.name}, guildId = ${event.guild?.id})" }
                when (event.subcommandName) {
                    "register" -> handleListenerRegister(event)
                    "remove" -> handleListenerRemove(event)
                    "status" -> handleStatus(event)
                }
            }
        }

        override fun onGuildReady(event: GuildReadyEvent) {
            // ID of Nomination Planner servers
            val commands = if (event.guild.id == "761157153884864532") {
                getCommand(developerServer = true)
            } else {
                getCommand(developerServer = false)
            }

            // Add the commands to the guild
            event.guild.updateCommands()
                .addCommands(commands)
                .queue()
        }

        private fun handleStatus(event: SlashCommandEvent) {
            event.reply("TODO status message").queue()
        }

        private fun handleListenerRemove(event: SlashCommandEvent) {
            event.deferReply().queue()

            val isAdmin = event.member?.permissions?.contains(Permission.ADMINISTRATOR) == true

            if (!isAdmin) {
                val failedEmbedMessage = EmbedBuilder()
                    .setDescription("**You need to be an administrator to execute this command**")
                    .setColor(Color.RED)
                    .build()

                event.hook.sendMessageEmbeds(failedEmbedMessage).queue()
                return
            }

            val guildId = event.guild!!.id
            val channelId = event.channel.id

            val existingListener = dataSource.findByGuildIdAndChannelId(guildId, channelId)
            if (existingListener == null) {
                val failedEmbedMessage = EmbedBuilder()
                    .setDescription("**ERROR: Could not remove listener!**\nNothing is registered for this channel")
                    .setColor(Color.RED)
                    .build()

                event.hook.sendMessageEmbeds(failedEmbedMessage).queue()
                return
            }

            val deleteResult = dataSource.remove(existingListener)
            if (deleteResult.deletedCount == 1L) {
                val embedMessage = EmbedBuilder()
                    .setDescription("**Removed listener**")
                    .setColor(Color.GREEN)
                    .build()

                event.hook.sendMessageEmbeds(embedMessage).queue()
            } else {
                val failedEmbedMessage = EmbedBuilder()
                    .setDescription("**ERROR: Could not remove listener!**\nPlease try again.\nIf this keeps occurring, contact Greaper")
                    .setColor(Color.RED)
                    .build()

                event.hook.sendMessageEmbeds(failedEmbedMessage).queue()
            }
        }

        private fun handleListenerRegister(event: SlashCommandEvent) {
            event.deferReply().queue()

            val gamemodeValue = event.getOption("gamemode")?.asString
            val gamemode = if (gamemodeValue != null) {
                Gamemode.valueOf(gamemodeValue)
            } else {
                null
            }

            val isAdmin = event.member?.permissions?.contains(Permission.ADMINISTRATOR) == true

            if (!isAdmin) {
                val failedEmbedMessage = EmbedBuilder()
                    .setDescription("**You need to be an administrator to execute this command**")
                    .setColor(Color.RED)
                    .build()

                event.hook.sendMessageEmbeds(failedEmbedMessage).queue()
                return
            }

            val replyMessage = StringBuilder("**Registered listener at channel**\n")
                .let { builder ->
                    if (gamemode != null) {
                        builder.append("Gamemode = `${gamemode.toReadableName()}`")
                    } else {
                        builder.append("Gamemode = `all`")
                    }
                }
                .toString()

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
            val success = if (existingListener == null) {
                dataSource.create(newListener).wasAcknowledged()
            } else {
                // Listener is already known for this channel, update it with the new configuration
                dataSource.replace(existingListener, newListener).modifiedCount == 1L
            }

            if (success) {
                val embedMessage = EmbedBuilder()
                    .setDescription(replyMessage)
                    .setColor(Color.GREEN)
                    .build()

                event.hook.sendMessageEmbeds(embedMessage).queue()
            } else {
                val failedEmbedMessage = EmbedBuilder()
                    .setDescription("**ERROR: Could not register listener!**\nPlease try again.\nIf this keeps occurring, contact Greaper")
                    .setColor(Color.RED)
                    .build()

                event.hook.sendMessageEmbeds(failedEmbedMessage).queue()
            }
        }

        private fun getCommand(developerServer: Boolean): CommandData {
            val gamemodeOption = OptionData(OptionType.STRING, "gamemode", "The osu! gamemode the event is for")
                .addChoices(Gamemode.values().map { Choice(it.toReadableName(), it.name) })

            val registerCommand = SubcommandData("register", "Register a listener of bnplanner events")
                .addOptions(gamemodeOption)

            val removeCommand = SubcommandData("remove", "Remove a listener of bnplanner events")

            val commandData = CommandData("planner", "All nomination planner slash commands")

            return if (developerServer) {
                val statusCommand = SubcommandData("status", "Return the current status of the Nomination Planner")

                commandData.addSubcommands(registerCommand, removeCommand, statusCommand)
            } else {
                commandData.addSubcommands(registerCommand, removeCommand)
            }
        }
    }
}
