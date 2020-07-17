package io.vyne.models.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.*
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.XpathAccessor

// This is deprecated, as it doesn't respect more advanced
// topics such as conditional field evaluation.
@Deprecated("Use TypedObjectFactory instead")
class JsonModelParser(val schema: Schema, private val mapper: ObjectMapper = DEFAULT_MAPPER) {
   companion object {
      val DEFAULT_MAPPER = jacksonObjectMapper()
   }

   fun parse(type: Type, json: String, conversionService: ConversionService = ConversionService.DEFAULT_CONVERTER, source:DataSource): TypedInstance {
      return when {
         !type.isCollection && isJsonArray(json) -> parseCollection(
            mapper.readValue<List<Map<String, Any>>>("${json}[]"),
            schema.type("${type.fullyQualifiedName}[]"),
            conversionService, source)
         type.isCollection -> parseCollection(mapper.readValue<List<Map<String, Any>>>(json), type, conversionService, source)
         else -> doParse(type,  mapper.readValue(json), conversionService = conversionService, source = source)
      }
   }

   fun doParse(type: Type, valueMap: Map<String, Any>, isCollection: Boolean = false, conversionService: ConversionService = ConversionService.DEFAULT_CONVERTER, source:DataSource): TypedInstance {
      if (type.isTypeAlias) {
         val aliasedType = type.aliasForType!!
         val parsedAliasType = doParse(aliasedType, valueMap, isCollection, conversionService, source)
         return if (isCollection) {
            val collection = parsedAliasType as TypedCollection
            val collectionMembersAsAliasedType = collection.map { it.withTypeAlias(type) }
            TypedCollection(type, collectionMembersAsAliasedType, source)
         } else {
            parsedAliasType.withTypeAlias(type)
         }

      }

      if (type.isScalar) {
         return parseScalarValue(valueMap, type, conversionService, source)
      } else if (isCollection) {
         return parseCollection(valueMap, type, conversionService, source)
      } else if (isSingleObject(valueMap)) {
         return doParse(type, valueMap.values.first() as Map<String, Any>, isCollection = false, conversionService = conversionService, source = source)
      }
      else {
         val attributeInstances = type.attributes
            .filter { (attributeName, field) ->
               getValuePath(field, valueMap, attributeName) != null
            }
            .map { (attributeName, field: Field) ->
               val attributePath = getValuePath(field, valueMap, attributeName)!!
               val valueFromMap = valueMap[attributePath]
               val attributeType = schema.type(field.type.parameterizedName)
               if (valueFromMap != null) {
                  attributeName to doParse(attributeType, mapOf(attributeName to valueFromMap), schema.type(field.type).isCollection, conversionService, source = source)
               } else {
                  attributeName to TypedNull(attributeType)
               }
            }.toMap()
         return TypedObject(type, attributeInstances, source)
      }
   }

   private fun getValuePath(field: Field, valueMap: Map<String, Any>, attributeName: AttributeName): String? {
      return if (field.accessor != null && field.accessor is XpathAccessor) {
         val path = field.accessor.expression.removePrefix("/")
         if (valueMap.containsKey(path)) {
            return path
         } else {
            return null
         }
      } else if (valueMap.containsKey(attributeName)) {
         return attributeName
      } else {
         null
      }
   }

   private fun isSingleObject(valueMap: Map<String, Any>): Boolean {
      return valueMap.size == 1 && valueMap.values.first() is Map<*, *>
   }

   private fun parseCollection(valueMap: Map<String, Any>, type: Type, conversionService: ConversionService, source:DataSource): TypedCollection {
      assert(valueMap.size == 1) { "Received a map with ${valueMap.size} entries, expecting only a single entry for a collection type!" }
      val key = valueMap.entries.first().key
      val value = valueMap.entries.first().value
      assert(value is Collection<*>) {
         "Received a collection when expecting a scalar type"
      }
      val collection = value as Collection<*>
      val values = collection.filterNotNull().map { doParse(type.typeParameters[0], mapOf(key to it), isCollection = false, conversionService = conversionService, source = source) }
      return TypedCollection(type, values)
   }

   private fun parseCollection(collection: Collection<Map<String, Any>>, type: Type, conversionService: ConversionService, source:DataSource): TypedCollection {
      if (!type.isCollection) {
         // TODO : Could just wrap this in an array..
         error("${type.name} is not a collection type")
      }
      val values = collection.map { doParse(type.collectionType!!, it, isCollection = false, conversionService = conversionService, source = source) }
      return TypedCollection(type, values)
   }

   private fun parseScalarValue(valueMap: Map<String, Any>, type: Type, conversionService: ConversionService, source:DataSource): TypedValue {
      assert(valueMap.size == 1) { "Received a map with ${valueMap.size} entries, expecting only a single entry for a scalar type!" }
      val value = valueMap.entries.first().value
      assert(value !is Collection<*>) {
         "Received a collection when expecting a scalar type"
      }
      return TypedValue.from(type, value, conversionService, source)
   }
}
