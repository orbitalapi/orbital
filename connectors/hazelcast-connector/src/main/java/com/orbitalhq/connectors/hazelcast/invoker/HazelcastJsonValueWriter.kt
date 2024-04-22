package com.orbitalhq.connectors.hazelcast.invoker

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hazelcast.core.HazelcastJsonValue
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.Schema

object HazelcastJsonValueWriter {
   val OBJECT_MAPPER = jacksonObjectMapper()
   fun getJsonValueAndKey(typedInstance: TypedObject, schema: Schema):Pair<Any, HazelcastJsonValue> {
      val json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(typedInstance.toRawObject())
      val jsonValue = HazelcastJsonValue(json)

      val (keyField) = findKeyField(typedInstance.type)
      val key = typedInstance[keyField].toRawObject() ?: error("Field $keyField is the @Id, but is null or not present")

      return key to jsonValue
   }
}
