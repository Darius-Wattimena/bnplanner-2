package nl.greaper.bnplanner.model.aiess

import com.fasterxml.jackson.annotation.JsonProperty

data class AiessUserEvent(
    val type: AiessUserEventType,
    @JsonProperty("userId")
    val osuUserId: String,
    @JsonProperty("groupId")
    val osuGroupId: String
)

enum class AiessUserEventType {
    ADD,
    REMOVE
}
