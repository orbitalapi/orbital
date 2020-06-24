package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.types.EnumType

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when Parsing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val conversionService: ConversionService = ConversionService.DEFAULT_CONVERTER) {
   fun parse(value: Any, targetType: Type): TypedInstance {
      if (targetType.isEnum) {
         return parseEnum(value, targetType)
      }
      return TypedValue.from(targetType, value, conversionService)
   }

   private fun parseEnum(value: Any, targetType: Type): TypedInstance {

      val enumType = targetType.taxiType as EnumType

      return if(enumType.has(value)){
         TypedValue.from(targetType, value, conversionService)
      } else {
         error("""
   Unable to map Value=${value} to Enum Type=${targetType.fullyQualifiedName}, allowed values=${enumType.definition?.values?.map { it.name to it.value }}
            """)
      }

   }
}

