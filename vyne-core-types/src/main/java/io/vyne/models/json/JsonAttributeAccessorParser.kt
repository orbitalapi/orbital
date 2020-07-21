package io.vyne.models.json

import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import io.vyne.models.DataSource
import io.vyne.models.PrimitiveParser
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.XpathAccessor

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParser(private val primitiveParser: PrimitiveParser = PrimitiveParser()) {
   fun parseToType(type: Type, accessor: XpathAccessor, record: ObjectNode, schema: Schema, source:DataSource): TypedInstance {
      val node = record.at(accessor.expression)
      if (node.isMissingNode) {
         error("Could not find xpath ${accessor.expression} in record")
      } else {
         val value = when {
            node.isNumber -> node.numberValue()
            node.isNull -> null
            node.isTextual && node.asText().trim().isEmpty() && type.isEnum -> null
            else -> node.asText()
         }

         if(value === null) {
            return TypedInstance.from(type,  null, schema, source = source);
         }

         return primitiveParser.parse(value, type, source)
      }
   }
}

