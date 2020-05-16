package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.PrimitiveType

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when Parsing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val objectMapper: ObjectMapper = jacksonObjectMapper()) {
   fun parse(value: Any, targetType: Type, schema: Schema): TypedInstance {
      val underlyingPrimitive = Primitives.getUnderlyingPrimitive(targetType, schema)
      val taxiPrimitive = PrimitiveType.fromDeclaration(underlyingPrimitive.fullyQualifiedName)
      val javaType = PrimitiveTypes.getJavaType(taxiPrimitive)
      val convertedValue = objectMapper.convertValue(value, javaType)
      return TypedValue.from(targetType, convertedValue, performTypeConversions = false)
   }
}

object Primitives {
   fun getUnderlyingPrimitive(type: Type, schema: Schema): Type {
      val primitiveCandidates = getUnderlyingPrimitiveIfExists(type, schema)
      return when {
         primitiveCandidates.isEmpty() -> error("Type ${type.fullyQualifiedName} is not mappable to a primitive type")
         primitiveCandidates.size > 1 -> error("Type ${type.fullyQualifiedName} ambiguously maps to multiple primitive types: ${primitiveCandidates.joinToString { it.fullyQualifiedName }}")
         else -> primitiveCandidates.first()
      }
   }

   private fun getUnderlyingPrimitiveIfExists(type: Type, schema: Schema, typesToIgnore: Set<Type> = emptySet()): Set<Type> {
      if (type.isPrimitive) {
         val actualPrimitive = when {
            PrimitiveType.isPrimitiveType(type.fullyQualifiedName) -> type
            type.isTypeAlias && PrimitiveType.isPrimitiveType(type.aliasForType!!.fullyQualifiedName) -> type.aliasForType!!
            else -> error("Type ${type.fullyQualifiedName} is marked as Primitive, but couldn't find an underlying primitive type")
         }
         return setOf(actualPrimitive)
      }

      val typesToConsider = (type.inheritanceGraph + type.aliasForType).filterNotNull()
      val recursiveTypesToIgnore = typesToIgnore + type
      val types = typesToConsider.flatMap { getUnderlyingPrimitiveIfExists(it, schema, recursiveTypesToIgnore) }.toSet()
      return types

   }
}
