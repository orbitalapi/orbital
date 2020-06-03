@file:Suppress("UNCHECKED_CAST")

package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import org.springframework.core.convert.ConverterNotFoundException
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.lang.Nullable
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

interface ConversionService {
   fun <T> convert(@Nullable source: Any?, targetType: Class<T>, format: String?): T

   companion object {
      fun default(): ConversionService {
         return StringToIntegerConverter(
            FormattedInstantConverter(
               VyneDefaultConversionService
            )
         )
      }
   }
}

/**
 * Used when you don't want to perform any conversions
 */
object NoOpConversionService : ConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>, format: String?): T {
      return source!! as T
   }
}

object VyneDefaultConversionService : ConversionService {
   private val innerConversionService by lazy {
      val service = DefaultConversionService()
      // TODO :  we need to be much richer about date handling.
      service.addConverter(String::class.java, LocalDate::class.java) { s -> LocalDate.parse(s) }
      service.addConverter(java.lang.Long::class.java, Instant::class.java) { s -> Instant.ofEpochMilli(s.toLong()) }
      // TODO Check this as it is a quick addition for the demo!
      service.addConverter(java.lang.Long::class.java, LocalDate::class.java) { s -> Instant.ofEpochMilli(s.toLong()).atZone(ZoneId.of("UTC")).toLocalDate(); }
      service
   }

   override fun <T> convert(source: Any?, targetType: Class<T>, format: String?): T {
      try {
         return innerConversionService.convert(source, targetType)!!
      } catch (e: ConverterNotFoundException) {
         throw IllegalArgumentException("Unable to convert value=${source} to type=${targetType} Error: ${e.message}", e)
      }
   }
}

interface ForwardingConversionService : ConversionService {
   val next: ConversionService
}

class FormattedInstantConverter(override val next: ConversionService = NoOpConversionService) : ForwardingConversionService {
   private fun toLocalDateTime(source: String, format: String?): LocalDateTime {
      require(format != null) { "Formats are expected for Instants" }
      // Note - using US Locale so that AM PM in uppercase is supported
      val locale = when {
         source.contains("pm") || source.contains("am") -> Locale.UK
         source.contains("PM") || source.contains("AM") -> Locale.US
         else -> Locale.getDefault()
      }
      val formatter = DateTimeFormatter.ofPattern(format, locale)
      return LocalDateTime.parse(source, formatter)
   }

   override fun <T> convert(source: Any?, targetType: Class<T>, format: String?): T {
      return when {
         source is String && targetType == Instant::class.java -> {
            toLocalDateTime(source, format).toInstant(ZoneOffset.UTC) as T  // TODO : We should be able to detect that from the format sometimes
         }
         source is String && targetType == LocalDateTime::class.java -> {
            toLocalDateTime(source, format) as T
         }
         else -> {
            next.convert(source, targetType, format)
         }
      }
   }

}

class StringToIntegerConverter(override val next: ConversionService = NoOpConversionService) : ForwardingConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>, format: String?): T {
      return if (source is String && targetType == Int::class.java) {
         BigDecimal(source).intValueExact() as T
      } else {
         next.convert(source, targetType, format)
      }
   }
}

data class TypedValue private constructor(override val type: Type, override val value: Any) : TypedInstance {
   companion object {
      private val conversionService by lazy {
         ConversionService.default()
      }

      fun from(type: Type, value: Any, converter: ConversionService): TypedValue {
         if (!type.taxiType.inheritsFromPrimitive) {
            error("Type ${type.fullyQualifiedName} is not a primitive, cannot be converted")
         } else {
            val valueToUse = converter.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!), type.format)
            return TypedValue(type, valueToUse)
         }

      }

      @Deprecated("Use conversionService approach")
      fun from(type: Type, value: Any, performTypeConversions: Boolean = true): TypedValue {
         val conversionServiceToUse = if (performTypeConversions) {
            conversionService
         } else {
            NoOpConversionService
         }
         return from(type, value, conversionServiceToUse)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value)
   }

   /**
    * Returns true if the two are equal, where the values are the same, and the underlying
    * types resolve to the same type, considering type aliases.
    */
   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedValue) {
         return false
      }
      if (!(this.type.resolvesSameAs(valueToCompare.type) || valueToCompare.type.inheritsFrom(this.type))) return false;
      return this.value == valueToCompare.value
   }

}
