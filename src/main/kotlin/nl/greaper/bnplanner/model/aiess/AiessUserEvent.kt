package nl.greaper.bnplanner.model.aiess

import com.fasterxml.jackson.annotation.JsonProperty
import nl.greaper.bnplanner.model.Gamemode

data class AiessUserEvent(
    /**
     * add | remove
     */
    val type: AiessUserEventType,

    /**
     * The osu ID of the user being moved.
     */
    val userId: String,

    /**
     * The current username of the user in osu.
     */
    @JsonProperty("userName")
    val username: String,

    /**
     * osu ID of the group the user is moved to/from.
     *
     * Supported groups:
     * - 7 = NAT
     * - 28 = BN
     * - 32 = PBN
     */
    val groupId: String,

    /**
     * osu | taiko | fruits | mania
     */
    @JsonProperty("gameMode")
    val gamemode: Gamemode
)

enum class AiessUserEventType {
    add,
    remove
}
