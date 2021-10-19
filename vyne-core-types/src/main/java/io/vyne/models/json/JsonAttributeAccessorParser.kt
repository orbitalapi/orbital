package io.vyne.models.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.vyne.models.DataSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.accessors.JsonPathAccessor

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParser(private val primitiveParser: PrimitiveParser = PrimitiveParser()) {
   companion object {
      val jsonPath = JsonPath.using(
         Configuration.defaultConfiguration()
            .mappingProvider(JacksonMappingProvider())
            .jsonProvider(JacksonJsonNodeJsonProvider())
      )
      val objectMapper: ObjectMapper = jacksonObjectMapper()
   }

   fun parseToType(type: Type, accessor: JsonPathAccessor, record: Map<*, *>, schema: Schema, source: DataSource): TypedInstance {
      return internalParseToType(type, accessor, record, schema, source)
   }

   fun parseToType(type: Type, accessor: JsonPathAccessor, record: ObjectNode, schema: Schema, source: DataSource): TypedInstance {
      return internalParseToType(type, accessor, record, schema, source)
   }

   // Internal method that relaxes type-safety around the record attribute.
   // The JsonPath parser can operate against either a Map or a ObjectNode, and we use both
   // However, I don't want a public "record: any" method, as it creates amiguity around the valid
   // types to pass here.
   private fun internalParseToType(type: Type, accessor: JsonPathAccessor, record: Any, schema: Schema, source: DataSource): TypedInstance {
      val expressionValue = if (accessor.expression.startsWith("/")) {
         when (record) {
            is Map<*,*> -> parseXpathStyleExpression(record, accessor, type, schema, source)
            is ObjectNode -> parseXpathStyleExpression(record, accessor, type, schema, source)
            else -> {
               log().error("Parsing xpath-style path (${accessor.expression}) is only supported against an ObjectNode or a Map.  It's an error that you got here.  Returning null")
               null
            }
         }

      } else {
         parseJsonPathStyleExpression(record, accessor, type, schema, source)
      }
      return if (expressionValue === null) {
         TypedNull.create(type, source)
      } else {
         primitiveParser.parse(expressionValue, type, source)
      }
   }

   private fun parseJsonPathStyleExpression(record: Any, accessor: JsonPathAccessor, type: Type, schema: Schema, source: DataSource): Any? {
      val jsonSource = when (record) {
         is JsonParsedStructure -> record.jsonNode
         is Map<*, *> -> objectMapper.valueToTree(record)
         else -> record
      }
      return try {
         val node = jsonPath.parse(jsonSource).read<Any>(accessor.expression) as JsonNode
         unwrapJsonNode(node, type, accessor)

      } catch (e: PathNotFoundException) {
         log().warn("Could not evaluate path ${accessor.expression} as a PathNotFoundException was thrown, will return null - ${e.message}")
         null
      }
   }

   private fun unwrapJsonNode(node: JsonNode, type: Type, accessor: JsonPathAccessor): Any? {
      return when {
         node.isNumber -> node.numberValue()
         node.isTextual -> node.textValue()
         node.isBoolean -> node.booleanValue()
         node.isArray -> {
            // Edge case - if we got passed what Jsonpath calls "indefinite" path, we'll get a collection here.
            // If the caller expects a single value, and the parser returned an array with a single item, use that.
            val arrayNode = node as ArrayNode
            when {
               type.isCollection -> arrayNode.arrayNode()
               arrayNode.size() == 0 -> null
               arrayNode.size() == 1 -> unwrapJsonNode(arrayNode[0], type, accessor)
               else -> {
                  log().error("JsonPath $accessor evaluated to a collection of ${arrayNode.size()} elements, but attempting to parse to a type of ${type.fullyQualifiedName}.  This is invalid.  Returning null")
                  null
               }

            }
         }
         else -> node.textValue()
      }
   }


   private fun parseXpathStyleExpression(record: Map<*,*>, accessor: JsonPathAccessor, type: Type, schema: Schema, source: DataSource): Any? {
      val propertyName = accessor.expression.removePrefix("/")
      return record[propertyName]
   }
   private fun parseXpathStyleExpression(record: ObjectNode, accessor: JsonPathAccessor, type: Type, schema: Schema, source: DataSource): Any? {
      val node = record.at(accessor.expression)
      return if (node.isMissingNode) {
         log().warn("Could not find json pointer ${accessor.expression} in record")
         null
      } else {
         when {
            node.isNumber -> node.numberValue()
            node.isNull -> null
            node.isTextual && node.asText().trim().isEmpty() && type.isEnum -> null
            else -> node.asText()
         }


      }
   }
}

