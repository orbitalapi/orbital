package com.orbitalhq.connectors.redis.serialization

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.Schema

object RedisJsonValueWriter {
   val OBJECT_MAPPER = jacksonObjectMapper()

   fun getJsonStringAndKey(typedInstance: TypedObject, schema: Schema): Pair<Any, String> {
      val json = OBJECT_MAPPER.writeValueAsString(typedInstance.toRawObject())

      val (keyField) = findKeyField(typedInstance.type)
      val key = typedInstance[keyField].toRawObject()
         ?: error("Field $keyField is the @Id, but is null or not present")

      return key to json
   }

   fun getJsonString(typedInstance: TypedObject): String {
      return OBJECT_MAPPER.writeValueAsString(typedInstance.toRawObject())
   }
}
