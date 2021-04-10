package dev.hbeck.alt.text

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import pbandk.ExperimentalProtoJson
import pbandk.Message
import pbandk.json.JsonConfig
import pbandk.json.encodeToJsonString
import java.io.IOException


class PBSerDeModule: SimpleModule() {
    init {
        addSerializer(PbandkJsonSerializer())
    }

    @OptIn(ExperimentalProtoJson::class)
    internal class PbandkJsonSerializer : StdSerializer<Message>(Message::class.java) {
        @Throws(IOException::class)
        override fun serialize(value: Message, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeRaw(value.encodeToJsonString(
                JsonConfig.DEFAULT.copy(
                outputProtoFieldNames = true,
                outputDefaultValues = true,
                outputDefaultStringsAsNull = true,
                ignoreUnknownFieldsInInput = true
            )))
        }
    }
}