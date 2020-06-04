package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.JsonNode
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.timed
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import java.util.concurrent.TimeUnit

class JsonStreamMapper(private val targetType: Type, private val schema: Schema) {
   private val versionedType = schema.versionedType(targetType.name)

   fun map(jsonRecord: JsonNode): InstanceAttributeSet {
      val instance = timed("JsonStreamMapper.map", false, timeUnit = TimeUnit.MILLISECONDS) {
         TypedInstance.from(targetType, jsonRecord, schema)
      }

      return InstanceAttributeSet(
         versionedType,
         instance.value as Map<String, TypedInstance>
      )
   }

}
