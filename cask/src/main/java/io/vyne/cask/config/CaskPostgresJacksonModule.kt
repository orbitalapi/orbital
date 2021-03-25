package io.vyne.cask.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.postgresql.util.PGobject

class CaskPostgresJacksonModule : SimpleModule("casks.postgres") {
   init {
       addSerializer(PGobject::class.java, PGObjectSerializer())
   }
}

/**
 * Converts PGobjects (the thing that jsonb columns are persisted as)
 * to json
 */
class PGObjectSerializer : JsonSerializer<PGobject>() {
   override fun serialize(pgObject: PGobject, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeRawValue(pgObject.value)
   }
}
