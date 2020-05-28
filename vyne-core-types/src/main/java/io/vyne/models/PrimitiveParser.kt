package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
class PrimitiveParser(private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())) {
   fun parse(value: Any, targetType: Type, schema: Schema): TypedInstance {
      if (targetType.isEnum) {
         return parseEnum(value, targetType)
      }
      val underlyingPrimitive = Primitives.getUnderlyingPrimitive(targetType, schema)
      val taxiPrimitive = PrimitiveType.fromDeclaration(underlyingPrimitive.fullyQualifiedName)
      val javaType = PrimitiveTypes.getJavaType(taxiPrimitive)
      //try {
         val convertedValue = objectMapper.convertValue(value, javaType) //TODO report field name?
         if (convertedValue == null) {
            throw IllegalArgumentException("Unable to parse primitive type=${targetType.taxiType.basePrimitive} name=${targetType.name} value=null.")
         }
         return TypedValue.from(targetType, convertedValue, performTypeConversions = false)
//      } catch (e: Exception) {
//         log().error("Value ${value} javaType=${javaType} taxiPrimitive=${taxiPrimitive} targetType=${targetType}  ", e)
//         throw e;
//      }
   }

   private fun parseEnum(value: Any, targetType: Type): TypedInstance {
      return when (targetType.enumValues.contains(value)) {
         true -> TypedValue.from(targetType, value, false)
         else -> error("Unable to map Value=${value} to Enum Type=${targetType.fullyQualifiedName}, allowed values=${targetType.enumValues}")
      }
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
