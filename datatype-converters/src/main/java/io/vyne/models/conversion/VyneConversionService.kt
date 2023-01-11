package io.vyne.models.conversion

import com.fasterxml.jackson.datatype.jsr310.DecimalUtils
import io.vyne.models.ConversionService
import io.vyne.models.NoOpConversionService
import lang.taxi.types.EnumValue
import lang.taxi.types.FormatsAndZoneOffset
import org.springframework.core.convert.ConverterNotFoundException
import org.springframework.core.convert.support.DefaultConversionService
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.function.BiFunction

object VyneConversionService : ConversionService {
   private val innerConversionService by lazy {
      StringToNumberConverter(
         FormattedInstantConverter(
            SpringConverterWrapper()
         )
      )
   }

   override fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T {
      try {
         return innerConversionService.convert(source, targetType, format)!!
      } catch (e: ConverterNotFoundException) {
         throw IllegalArgumentException(
            "Unable to convert value=${source} to type=${targetType} Error: ${e.message}",
            e
         )
      }
   }
}

interface ForwardingConversionService : ConversionService {
   val next: ConversionService
}

private class SpringConverterWrapper : ConversionService {

   val innerConversionService = buildSpringConversionService()
   override fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T {
      try {
         return innerConversionService.convert(source, targetType)!!
      } catch (e: ConverterNotFoundException) {
         throw IllegalArgumentException(
            "Unable to convert value=${source} to type=${targetType} Error: ${e.message}",
            e
         )
      }
   }

   private fun buildSpringConversionService(): DefaultConversionService {
      val service = DefaultConversionService()
      // TODO :  we need to be much richer about date handling.
      service.addConverter(String::class.java, LocalDate::class.java) { s -> LocalDate.parse(s) }
      service.addConverter(java.lang.Long::class.java, Instant::class.java) { s -> Instant.ofEpochMilli(s.toLong()) }
      // TODO Check this as it is a quick addition for the demo!
      service.addConverter(java.lang.Long::class.java, LocalDate::class.java) { s ->
         Instant.ofEpochMilli(s.toLong()).atZone(ZoneId.of("UTC")).toLocalDate()
      }
      service.addConverter(
         java.lang.Long::class.java,
         LocalDateTime::class.java
      ) { s -> Instant.ofEpochMilli(s.toLong()).atZone(ZoneId.of("UTC")).toLocalDateTime(); }
      service.addConverter(java.lang.Double::class.java, Instant::class.java) { instantAsSecondsAndNanoSeconds ->
         val decimalValue = BigDecimal.valueOf(instantAsSecondsAndNanoSeconds.toDouble())
         DecimalUtils.extractSecondsAndNanos(decimalValue, BiFunction { s: Long, ns: Int ->
            Instant.ofEpochSecond(s, ns.toLong())
         })
      }
      service.addConverter(java.lang.Double::class.java, LocalDateTime::class.java) { instantAsSecondsAndNanoSeconds ->
         val decimalValue = BigDecimal.valueOf(instantAsSecondsAndNanoSeconds.toDouble())
         val extractedInstant = DecimalUtils.extractSecondsAndNanos(decimalValue, BiFunction { s: Long, ns: Int ->
            Instant.ofEpochSecond(s, ns.toLong())
         })
         extractedInstant.atZone(ZoneId.of("UTC")).toLocalDateTime()
      }
      service.addConverter(java.lang.Double::class.java, LocalDate::class.java) { instantAsSecondsAndNanoSeconds ->
         val decimalValue = BigDecimal.valueOf(instantAsSecondsAndNanoSeconds.toDouble())
         val extractedInstant = DecimalUtils.extractSecondsAndNanos(decimalValue, BiFunction { s: Long, ns: Int ->
            Instant.ofEpochSecond(s, ns.toLong())
         })
         extractedInstant.atZone(ZoneId.of("UTC")).toLocalDate()
      }
      service.addConverter(EnumValue::class.java, String::class.java) { s -> s.qualifiedName }
      return service
   }

}

class FormattedInstantConverter(override val next: ConversionService = NoOpConversionService) :
   ForwardingConversionService {
   private fun <D> toTemporalObject(
      source: String,
      format: List<String>?,
      doConvert: (source: String, formatter: DateTimeFormatter) -> D,
      optionalFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
   ): D {
      require(format != null) {
         " Formats are expected for Date types"
      }
      // Note - using US Locale so that AM PM in uppercase is supported
      val locale = when {
         source.contains("pm") || source.contains("am") -> Locale.UK
         source.contains("PM") || source.contains("AM") -> Locale.US
         else -> Locale.getDefault()
      }

      val formatterBuilder = DateTimeFormatterBuilder()
      format.forEach { f ->
         formatterBuilder.appendOptional(
            DateTimeFormatterBuilder()
               .parseLenient()
               .parseCaseInsensitive()
               .appendPattern(f)
               .toFormatter(locale)
         )
      }

      val formatter = formatterBuilder
         .parseLenient()
         .appendOptional(optionalFormatter)
         .toFormatter()

      return doConvert(source, formatter)
   }


   override fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T {
      return when {
         source is String && targetType == Instant::class.java -> {
            toTemporalObject(source, format?.patterns, UtcAsDefaultInstantConverter::parse) as T
         }
         source is String && targetType == LocalDateTime::class.java -> {
            toTemporalObject(source, format?.patterns, LocalDateTime::parse) as T
         }
         source is String && targetType == LocalDate::class.java -> {
            toTemporalObject(source, format?.patterns, LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE) as T
         }
         source is String && targetType == LocalTime::class.java -> {
            toTemporalObject(source, format?.patterns, LocalTime::parse, DateTimeFormatter.ISO_TIME) as T
         }
         else -> {
            next.convert(source, targetType, format)
         }
      }
   }
}

/**
 * Converts to an instant.  If a zone string is present in the format string(s),
 * then that's used.  Otherwise, UTC is assumed
 */
private object UtcAsDefaultInstantConverter {
   fun parse(source: String, formatter: DateTimeFormatter): Instant {
      val hasZone = formatter.parse(source).isSupported(ChronoField.OFFSET_SECONDS)
      return if (hasZone) {
         ZonedDateTime.parse(source, formatter).toInstant()
      } else {
         LocalDateTime.parse(source, formatter).toInstant(ZoneOffset.UTC)
      }
   }
}

class StringToNumberConverter(override val next: ConversionService = NoOpConversionService) :
   ForwardingConversionService {
   override fun <T> convert(source: Any?, targetType: Class<T>, format: FormatsAndZoneOffset?): T {
      if (source !is String) {
         return next.convert(source, targetType, format)
      } else {
         val numberFormat = NumberFormat.getInstance(Locale.ENGLISH)
         return when (targetType) {
            Int::class.java -> fromScientific(source)?.toInt() as T ?: numberFormat.parse(source).toInt() as T
            Double::class.java -> fromScientific(source)?.toDouble() as T ?: numberFormat.parse(source).toDouble() as T
            BigDecimal::class.java -> {
               val scientificValue = fromScientific(source)
               when {
                  scientificValue != null -> {
                     scientificValue as T
                  }
                  numberFormat is DecimalFormat -> {
                     numberFormat.isParseBigDecimal = true
                     numberFormat.parse(source) as T
                  }
                  else -> {
                     TODO("Didn't receive a decimal formatter from the locale")
                  }
               }
            }
            else -> next.convert(source, targetType, format)
         }
      }
   }

   private fun fromScientific(source: String): BigDecimal? {
      return if (source.contains("E") || source.contains("e")) {
         BigDecimal(source)
      } else {
         null
      }
   }
}
