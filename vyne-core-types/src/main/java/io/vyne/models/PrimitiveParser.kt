package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.types.EnumType

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when Parsing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val conversionService: ConversionService = ConversionService.default()) {
   fun parse(value: Any, targetType: Type): TypedInstance {
      if (targetType.isEnum) {
         return parseEnum(value, targetType)
      }
      return TypedValue.from(targetType, value, conversionService)
   }

   private fun parseEnum(value: Any, targetType: Type): TypedInstance {
      return when (targetType.enumValues.contains(value)) {
         true -> TypedValue.from(targetType, value, conversionService)
         else -> {
            // TODO fix me, vyne type should have enum values https://projects.notional.uk/youtrack/issue/LENS-131
            val taxiType = (targetType.taxiType as EnumType)
            val taxiEnumName = taxiType.values.find { it.value == value }?.name
            taxiEnumName
               ?.let { TypedValue.from(targetType, it, conversionService) }
               ?: error("Unable to map Value=${value} " +
                  "to Enum Type=${targetType.fullyQualifiedName}, " +
                  "allowed values=${taxiType.definition?.values?.map { Pair(it.name, it.value) }}")
         }
      }
   }
}

