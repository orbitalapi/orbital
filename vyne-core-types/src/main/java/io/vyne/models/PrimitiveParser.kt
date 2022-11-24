package io.vyne.models

import io.vyne.schemas.Type

/**
 * Responsible for simple conversions between primitives.
 * Cannot parse full objects.
 *
 * Used when Parsing some non type-safe wire format (eg., xpath returning a number as a string)
 */
class PrimitiveParser(private val conversionService: ConversionService = ConversionService.DEFAULT_CONVERTER) {
   fun parse(
      value: Any,
      targetType: Type,
      source: DataSource,
      parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException
   ): TypedInstance {
      if (targetType.isEnum) {
         return parseEnum(value, targetType, source)
      }
      return TypedValue.from(targetType, value, conversionService, source, parsingErrorBehaviour, targetType.formatAndZoneOffset)
   }

   private fun parseEnum(value: Any, targetType: Type, source: DataSource): TypedInstance {
      if (value is Boolean) {
         return parseEnum(value.toString(), targetType, source)
      }
      return targetType.enumTypedInstance(value, source)
//      val enumType = targetType.taxiType as EnumType
//      val typedInstance = when {
//         enumType.resolvesToDefault(value) -> {
//            // An enum type has a default, and won't match the other values,
//            // so generate from the default
//            TypedValue.from(targetType, enumType.of(value).name, conversionService, source)
//         }
//         enumType.has(value) -> TypedValue.from(targetType, value, conversionService, source)
//         // TODO push this logic to taxi.
//         value.toString().toIntOrNull() != null -> enumType.values
//            .filter { it.value == value.toString().toIntOrNull() }
//            .map { TypedValue.from(targetType, value, conversionService, source) }
//            .firstOrNull()
//         else -> null
//      }
//
//      return typedInstance ?: error("""
//   Unable to map Value=${value} to Enum Type=${targetType.fullyQualifiedName}, allowed values=${enumType.definition?.values?.map { it.name to it.value }}
//            """)

   }
}

