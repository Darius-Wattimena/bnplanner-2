package nl.greaper.bnplanner.model.aiess

import nl.greaper.bnplanner.model.Gamemode

data class AiessUserEvent(
    val type: AiessUserEventType,
    val userId: String,
    val username: String,
    val groupId: String,
    val gamemode: Gamemode
)

enum class AiessUserEventType {
    add,
    remove
}
