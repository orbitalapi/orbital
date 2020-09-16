package io.vyne.models.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.vyne.models.DataSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.JsonPathAccessor
import lang.taxi.types.XpathAccessor

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParser(private val primitiveParser: PrimitiveParser = PrimitiveParser()) {
   companion object {
      val jsonPath =  JsonPath.using(
         Configuration.defaultConfiguration()
            .mappingProvider(JacksonMappingProvider())
            .jsonProvider(JacksonJsonNodeJsonProvider())
      )


   }
   fun parseToType(type: Type, accessor: JsonPathAccessor, record: ObjectNode, schema: Schema, source:DataSource): TypedInstance {
      val expressionValue = if (accessor.expression.startsWith("/")) {
         parseXpathStyleExpression(record, accessor, type, schema, source)
      } else {
         parseJsonPathStyleExpression(record, accessor, type, schema, source)
      }
      if (expressionValue === null) {
         return TypedInstance.from(type, null, schema, source = source);
      }

      return primitiveParser.parse(expressionValue, type, source)

   }

   private fun parseJsonPathStyleExpression(record: ObjectNode, accessor: JsonPathAccessor, type: Type, schema: Schema, source: DataSource): Any? {

      return try {
         val node = jsonPath.parse(record).read<Any>(accessor.expression) as JsonNode
         when {
            node.isNumber -> node.numberValue()
            node.isTextual -> node.textValue()
            node.isBoolean -> node.booleanValue()
            else -> node.textValue()
         }
      } catch (e: PathNotFoundException) {
         log().warn("Could not evaluate path ${accessor.expression}, will return null - ${e.message}")
         null
      }
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

