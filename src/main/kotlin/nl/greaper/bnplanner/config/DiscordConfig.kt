package nl.greaper.bnplanner.config

import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscordConfig {

    @Bean
    fun jda(properties: DiscordProperties): JDA {
        return JDABuilder.createDefault(properties.token)
            .addEventListeners(DiscordBotHandler)
            .build()
    }

    object DiscordBotHandler : ListenerAdapter() {
        val log = KotlinLogging.logger { }

        override fun onSlashCommand(event: SlashCommandEvent) {
            if (event.name == "bnplanner" && event.subcommandName == "test") {
                log.info { "Handling discord message" }
                event.reply("This is a reply").queueAndLog()
            }
        }

        private val command = CommandData("bnplanner", "Test command")
            .addSubcommands(listOf(SubcommandData("test", "Test sub command")))

        override fun onGuildReady(event: GuildReadyEvent) {
            // FIXME remove, currently for testing
            // ID of Nomination Planner servers
            if (event.guild.id == "761157153884864532") {
                event.guild.updateCommands()
                    .addCommands(command)
                    .queue()
            }
        }

        private fun ReplyAction.queueAndLog() {
            this.queue { message ->
                log.info { message }
            }
        }
    }
}