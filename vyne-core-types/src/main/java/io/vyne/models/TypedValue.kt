@file:Suppress("UNCHECKED_CAST")

package io.vyne.models

import io.vyne.schemas.Type
import lang.taxi.Equality
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.EnumValue
import org.springframework.core.convert.ConverterNotFoundException
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.lang.Nullable
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

interface ConversionService {
   fun <T> convert(@Nullable source: Any?, targetType: Class<T>, format: List<String>?): T

   companion object {
      val DEFAULT_CONVERTER by lazy { newDefaultConverter() }

      /**
       * Creates a default converter.
       * Use this if you wish to have a new instance that you further customize.
       * If you're not planning on customizing, use DEFAULT_CONVERTER
       */
      fun newDefaultConverter(): ConversionService {
         return StringToNumberConverter(
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
   override fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T {
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
      service.addConverter(java.lang.Long::class.java, LocalDateTime::class.java) { s -> Instant.ofEpochMilli(s.toLong()).atZone(ZoneId.of("UTC")).toLocalDateTime(); }
      service.addConverter(EnumValue::class.java, String::class.java) { s -> s.qualifiedName }
      service
   }

   override fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T {
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
   private fun <D> toTemporalObject(
      source: String,
      format: List<String>?,
      doConvert: (source: String, formatter: DateTimeFormatter) -> D,
      optionalFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
   ): D {
      require(format != null) { "Formats are expected for Instants" }
      // Note - using US Locale so that AM PM in uppercase is supported
      val locale = when {
         source.contains("pm") || source.contains("am") -> Locale.UK
         source.contains("PM") || source.contains("AM") -> Locale.US
         else -> Locale.getDefault()
      }

      val formatterBuilder = DateTimeFormatterBuilder()
      format.forEach { f ->
         formatterBuilder.appendOptional(DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(f)
            .toFormatter(locale))
      }

      val formatter = formatterBuilder
         .parseLenient()
         .appendOptional(optionalFormatter)
         .toFormatter()
      return doConvert(source, formatter)
   }


   override fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T {
      return when {
         source is String && targetType == Instant::class.java -> {
            toTemporalObject(source, format, LocalDateTime::parse).toInstant(ZoneOffset.UTC) as T  // TODO : We should be able to detect that from the format sometimes
         }
         source is String && targetType == LocalDateTime::class.java -> {
            toTemporalObject(source, format, LocalDateTime::parse) as T
         }
         source is String && targetType == LocalDate::class.java -> {
            toTemporalObject(source, format, LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE) as T
         }
         source is String && targetType == LocalTime::class.java -> {
            toTemporalObject(source, format, LocalTime::parse, DateTimeFormatter.ISO_TIME) as T
         }
         else -> {
            next.convert(source, targetType, format)
         }
      }
   }
}

class StringToNumberConverter(override val next: ConversionService = NoOpConversionService) : ForwardingConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>, format: List<String>?): T {
      if (source !is String) {
         return next.convert(source, targetType, format)
      } else {
         val numberFormat = NumberFormat.getInstance()
         return when (targetType) {
            Int::class.java -> numberFormat.parse(source).toInt() as T
            Double::class.java -> numberFormat.parse(source).toDouble() as T
            BigDecimal::class.java -> {
               if (numberFormat is DecimalFormat) {
                  numberFormat.isParseBigDecimal = true
                  numberFormat.parse(source) as T
               } else {
                  TODO("Didn't receive a decimal formatter from the locale")
               }
            }
            else -> next.convert(source, targetType, format)
         }
      }

   }
}

data class TypedValue private constructor(override val type: Type, override val value: Any, override val source: DataSource) : TypedInstance {
   private val equality = Equality(this, TypedValue::type, TypedValue::value)

   companion object {
      private val conversionService by lazy {
         ConversionService.newDefaultConverter()
      }

      fun from(type: Type, value: Any, converter: ConversionService, source: DataSource): TypedValue {
         if (!type.taxiType.inheritsFromPrimitive) {
            error("Type ${type.fullyQualifiedName} is not a primitive, cannot be converted")
         } else {
            try {
               val valueToUse = converter.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!), type.format)
               return TypedValue(type, valueToUse, source)
            } catch (exception: Exception) {
               throw DataParsingException("Failed to parse value $value to type ${type.longDisplayName} - ${exception.message}", exception)
            }
         }

      }

      @Deprecated("Use conversionService approach")
      fun from(type: Type, value: Any, performTypeConversions: Boolean = true, source: DataSource): TypedValue {
         val conversionServiceToUse = if (performTypeConversions) {
            conversionService
         } else {
            NoOpConversionService
         }
         return from(type, value, conversionServiceToUse, source)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value, source)
   }

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()

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

class DataParsingException(message: String, exception: Exception) : RuntimeException(message)
