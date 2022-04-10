package nl.greaper.bnplanner.model.discord

import nl.greaper.bnplanner.model.discord.EmbedColor.AQUA
import nl.greaper.bnplanner.model.discord.EmbedColor.BLUE
import nl.greaper.bnplanner.model.discord.EmbedColor.DARKER_GREY
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_AQUA
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_BLUE
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_GOLD
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_GREEN
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_GREY
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_NAVY
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_ORANGE
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_PURPLE
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_RED
import nl.greaper.bnplanner.model.discord.EmbedColor.DARK_VIVID_PINK
import nl.greaper.bnplanner.model.discord.EmbedColor.GOLD
import nl.greaper.bnplanner.model.discord.EmbedColor.GREEN
import nl.greaper.bnplanner.model.discord.EmbedColor.GREY
import nl.greaper.bnplanner.model.discord.EmbedColor.LIGHT_GREY
import nl.greaper.bnplanner.model.discord.EmbedColor.LUMINOUS_VIVID_PINK
import nl.greaper.bnplanner.model.discord.EmbedColor.NAVY
import nl.greaper.bnplanner.model.discord.EmbedColor.ORANGE
import nl.greaper.bnplanner.model.discord.EmbedColor.PURPLE
import nl.greaper.bnplanner.model.discord.EmbedColor.RED

data class EmbedMessage(
    val description: String,
    //val timestamp: String,
    val color: Int,
    val thumbnail: EmbedThumbnail,
    val footer: EmbedFooter
)

data class EmbedThumbnail(val url: String)
data class EmbedFooter(val text: String, val icon_url: String? = null)

enum class EmbedColor {
    AQUA,
    GREEN,
    BLUE,
    PURPLE,
    GOLD,
    ORANGE,
    RED,
    GREY,
    DARKER_GREY,
    NAVY,
    DARK_AQUA,
    DARK_GREEN,
    DARK_BLUE,
    DARK_PURPLE,
    DARK_GOLD,
    DARK_ORANGE,
    DARK_RED,
    DARK_GREY,
    LIGHT_GREY,
    DARK_NAVY,
    LUMINOUS_VIVID_PINK,
    DARK_VIVID_PINK
}

fun EmbedColor.getValue(): Int {
    return when (this) {
        AQUA -> 1752220
        GREEN -> 3066993
        BLUE -> 3447003
        PURPLE -> 10181046
        GOLD -> 15844367
        ORANGE -> 15105570
        RED -> 15158332
        GREY -> 9807270
        DARKER_GREY -> 8359053
        NAVY -> 3426654
        DARK_AQUA -> 1146986
        DARK_GREEN -> 2067276
        DARK_BLUE -> 2123412
        DARK_PURPLE -> 7419530
        DARK_GOLD -> 12745742
        DARK_ORANGE -> 11027200
        DARK_RED -> 10038562
        DARK_GREY -> 9936031
        LIGHT_GREY -> 12370112
        DARK_NAVY -> 2899536
        LUMINOUS_VIVID_PINK -> 16580705
        DARK_VIVID_PINK -> 12320855
    }
}
