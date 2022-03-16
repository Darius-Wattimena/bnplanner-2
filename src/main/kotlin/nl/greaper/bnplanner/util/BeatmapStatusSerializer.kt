package nl.greaper.bnplanner.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus
import nl.greaper.bnplanner.model.beatmap.BeatmapStatus.Companion.toPriorityStatus

class BeatmapStatusSerializer : StdSerializer<BeatmapStatus>(BeatmapStatus::class.java) {
    override fun serialize(status: BeatmapStatus?, gen: JsonGenerator, provider: SerializerProvider) {
        val parsedStatus = status?.toPriorityStatus() ?: return

        gen.writeNumber(parsedStatus)
    }
}

class BeatmapStatusDeserializer : StdDeserializer<BeatmapStatus>(BeatmapStatus::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BeatmapStatus? {
        val value = parser.intValue

        return BeatmapStatus.fromPriorityStatus(value)
    }
}
