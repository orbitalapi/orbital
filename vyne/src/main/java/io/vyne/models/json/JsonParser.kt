package io.vyne.models.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.ModelContainer
import io.vyne.models.*
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.types.PrimitiveType

object RelaxedJsonMapper {
   val jackson: ObjectMapper = jacksonObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
}

fun ModelContainer.addJsonModel(typeName: String, json: String): TypedInstance {
   val model = parseJsonModel(typeName, json)
   this.addModel(model)
   return model
}

fun ModelContainer.parseJsonModel(typeName: String, json: String): TypedInstance {
   return jsonParser().parse(this.getType(typeName.fqn().parameterizedName), json)
}


fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}

class JsonModelParser(val schema: Schema, private val mapper: ObjectMapper = jacksonObjectMapper()) {
   fun parse(type: Type, json: String): TypedInstance {
      return if (type.fullyQualifiedName == PrimitiveType.ARRAY.qualifiedName) {
         val map = mapper.readValue<List<Map<String, Any>>>(json)
         parseCollection(map, type.typeParameters[0])
      } else {
         val map = mapper.readValue<Map<String, Any>>(json)
         doParse(type, map)
      }
   }

   internal fun doParse(type: Type, valueMap: Map<String, Any>, isCollection: Boolean = false): TypedInstance {
      if (type.isTypeAlias) {
         val aliasedType = type.aliasForType!!
         val parsedAliasType = doParse(aliasedType, valueMap, isCollection)
         return if (isCollection) {
             val collection = parsedAliasType as TypedCollection
             val collectionMembersAsAliasedType = collection.map { it.withTypeAlias(type) }
            TypedCollection(type,collectionMembersAsAliasedType)
         } else {
            parsedAliasType.withTypeAlias(type)
         }

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
            .map { (attributeName, field: Field) ->
               val attributeType = schema.type(field.type.parameterizedName)
               if (valueMap.containsKey(attributeName) && valueMap[attributeName] != null) {
                  attributeName to doParse(attributeType, mapOf(attributeName to valueMap.getValue(attributeName)), schema.type(field.type).isCollection)
               } else {
                  attributeName to TypedNull(attributeType)
               }
            }.toMap()
         return TypedObject(type, attributeInstances)
      }
   }

   private fun isSingleObject(valueMap: Map<String, Any>): Boolean {
      return valueMap.size == 1 && valueMap.values.first() is Map<*, *>
   }

   private fun parseCollection(valueMap: Map<String, Any>, type: Type): TypedCollection {
      assert(valueMap.size == 1) { "Received a map with ${valueMap.size} entries, expecting only a single entry for a collection type!" }
      val key = valueMap.entries.first().key
      val value = valueMap.entries.first().value
      assert(value is Collection<*>) {
         "Received a collection when expecting a scalar type"
      }
      val collection = value as Collection<*>
      val values = collection.filterNotNull().map { doParse(type.typeParameters[0], mapOf(key to it), isCollection = false) }
      return TypedCollection(type, values)
   }

   private fun parseCollection(collection: Collection<Map<String, Any>>, type: Type): TypedCollection {
      val values = collection.map { doParse(type, it, isCollection = false) }
      return TypedCollection(type, values)
   }

   private fun parseScalarValue(valueMap: Map<String, Any>, type: Type): TypedValue {
      assert(valueMap.size == 1) { "Received a map with ${valueMap.size} entries, expecting only a single entry for a scalar type!" }
      val value = valueMap.entries.first().value
      assert(value !is Collection<*>) {
         "Received a collection when expecting a scalar type"
      }
      return TypedValue.from(type, value, performTypeConversions = true)
   }
}
