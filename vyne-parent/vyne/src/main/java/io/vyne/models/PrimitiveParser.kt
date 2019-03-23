package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.PrimitiveType

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when parssing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val objectMapper: ObjectMapper = jacksonObjectMapper()) {
   fun parse(value: Any, targetType: Type): TypedInstance {
      if (!targetType.isPrimitive) {
         // Nte: We may need to consider alias chains
         error("${targetType.name} is not primitive, and cannot be parsed using this parser. ")
      }

      val underlyingTypeName = targetType.aliasForType ?: targetType.name
      val taxiPrimitive = PrimitiveType.fromDeclaration(underlyingTypeName.fullyQualifiedName)
      val javaType = PrimitiveTypes.getJavaType(taxiPrimitive)
      val convertedValue = objectMapper.convertValue(value, javaType)
      return TypedValue(targetType, convertedValue)
   }
}
