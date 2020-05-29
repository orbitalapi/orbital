package io.vyne.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.EnumType
import lang.taxi.types.PrimitiveType

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when Parsing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val conversionService: ConversionService = ConversionService.default()) {
   fun parse(value: Any, targetType: Type, schema: Schema): TypedInstance {
      if (targetType.isEnum) {
         return parseEnum(value, targetType)
      }
      // TODO fix me https://projects.notional.uk/youtrack/issue/LENS-128
      val inheritsFromEnum = targetType.inherits.filter { it.isEnum }
      if (inheritsFromEnum.isNotEmpty()) {
         return parseEnum(value, inheritsFromEnum.first())
      }
      return parsePrimitive(value, targetType, schema)
   }

   private fun parsePrimitive(value: Any, targetType: Type, schema: Schema): TypedValue {
      val underlyingPrimitive = Primitives.getUnderlyingPrimitive(targetType, schema)
      val taxiPrimitive = PrimitiveType.fromDeclaration(underlyingPrimitive.fullyQualifiedName)
      val javaType = PrimitiveTypes.getJavaType(taxiPrimitive)
      val convertedValue = conversionService.convert(value,javaType,targetType.format)
      if (convertedValue == null) {
         throw IllegalArgumentException("Unable to parse primitive type=${targetType.taxiType.basePrimitive} name=${targetType.name} value=null.")
      }
      return TypedValue.from(targetType, convertedValue, performTypeConversions = false)
   }

   private fun parseEnum(value: Any, targetType: Type): TypedInstance {
      return when (targetType.enumValues.contains(value)) {
         true -> TypedValue.from(targetType, value, false)
         else -> {
            // TODO fix me, vyne type should have enum values https://projects.notional.uk/youtrack/issue/LENS-131
            val taxiType = (targetType.taxiType as EnumType)
            val taxiEnumName = taxiType.values.find { it.value == value }?.name
            taxiEnumName
               ?.let { TypedValue.from(targetType, it, false) }
               ?: error("Unable to map Value=${value} " +
                  "to Enum Type=${targetType.fullyQualifiedName}, " +
                  "allowed values=${taxiType.definition?.values?.map { Pair(it.name, it.value) }}")
         }
      }
   }
}

object Primitives {
   fun getUnderlyingPrimitive(type: Type, schema: Schema): Type {

      return when {
         type.taxiType.basePrimitive == null -> {
            error("Type ${type.fullyQualifiedName} is not mappable to a primitive type")
         }
         else -> schema.type(type.taxiType.basePrimitive!!.qualifiedName)
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
