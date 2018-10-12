package io.vyne.models.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.osmosis.polymer.ModelContainer
import io.osmosis.polymer.models.TypedCollection
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type
import io.osmosis.polymer.schemas.TypeReference

fun ModelContainer.addJsonModel(typeName: String, json: String): TypedInstance {
   val model = parseJsonModel(typeName, json)
   this.addModel(model)
   return model
}

fun ModelContainer.parseJsonModel(typeName: String, json: String): TypedInstance {
   return jsonParser().parse(this.getType(typeName), json)
}


fun ModelContainer.jsonParser(mapper: ObjectMapper = jacksonObjectMapper()): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}

class JsonModelParser(val schema: Schema, val mapper: ObjectMapper = jacksonObjectMapper()) {
   fun parse(type: Type, json: String): TypedInstance {
      val map = mapper.readValue<Map<String, Any>>(json)
      return doParse(type, map)
   }

   internal fun doParse(type: Type, valueMap: Map<String, Any>, isCollection: Boolean = false): TypedInstance {
      if (type.isTypeAlias) {
         val aliasedType = schema.type(type.aliasForType!!)
         val parsedAliasType = doParse(aliasedType,valueMap,isCollection)
         return parsedAliasType.withTypeAlias(type)
      }

      if (type.isScalar && !isCollection) {
         return parseScalarValue(valueMap, type)
      } else if (isCollection) {
         return parseCollection(valueMap, type)
      } else if (isSingleObject(valueMap)) {
         return doParse(type, valueMap.values.first() as Map<String, Any>, isCollection = false)
      } else {
         val attributeInstances = type.attributes
            .filterKeys { attributeName -> valueMap.containsKey(attributeName) }
            .map { (attributeName, attributeTypeRef: TypeReference) ->
               val attributeType = schema.type(attributeTypeRef.name)
               attributeName to doParse(attributeType, mapOf(attributeName to valueMap[attributeName]!!), isCollection = attributeTypeRef.isCollection)
            }.toMap()
         return TypedObject(type, attributeInstances)
      }
   }

   private fun isSingleObject(valueMap: Map<String, Any>): Boolean {
      return valueMap.size == 1 && valueMap.values.first() is Map<*, *>
   }

   private fun parseCollection(valueMap: Map<String, Any>, type: Type): TypedCollection {
      assert(valueMap.size == 1, { "Received a map with ${valueMap.size} entries, expecting only a single entry for a collection type!" })
      val key = valueMap.entries.first().key
      val value = valueMap.entries.first().value
      assert(value is Collection<*>, {
         "Received a collection when expecting a scalar type"
      })
      val collection = value as Collection<*>
      val values = collection.filterNotNull().map { doParse(type, mapOf(key to it), isCollection = false) }
      return TypedCollection(type, values)
   }

   private fun parseScalarValue(valueMap: Map<String, Any>, type: Type): TypedValue {
      assert(valueMap.size == 1, { "Received a map with ${valueMap.size} entries, expecting only a single entry for a scalar type!" })
      val value = valueMap.entries.first().value
      assert(value !is Collection<*>, {
         "Received a collection when expecting a scalar type"
      })
      return TypedValue(type, value)
   }
}
