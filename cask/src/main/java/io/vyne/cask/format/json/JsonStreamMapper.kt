package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.JsonNode
import io.vyne.cask.batchTimed
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import java.util.concurrent.TimeUnit

class JsonStreamMapper(private val versionedType: VersionedType, private val schema: Schema) {
   fun map(jsonRecord: JsonNode, messagedId:String): InstanceAttributeSet {
      val instance = batchTimed("JsonStreamMapper.map", timeUnit = TimeUnit.MILLISECONDS) {
         TypedInstance.from(versionedType.type, jsonRecord, schema, source = Provided)
      }

      return InstanceAttributeSet(
         versionedType,
         instance.value as Map<String, TypedInstance>,
         messagedId
      )
   }

}
