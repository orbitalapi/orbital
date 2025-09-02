package com.orbitalhq.models.conversion

import org.springframework.core.convert.converter.Converter
import org.springframework.lang.NonNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * This is a copy of Spring Data Commons' Jsr310Converters
 */
object Jsr310Converters {
   private val CLASSES: List<Class<*>> = java.util.List.of<Class<*>>(
      LocalDateTime::class.java,
      LocalDate::class.java,
      LocalTime::class.java,
      Instant::class.java,
      ZoneId::class.java,
      Duration::class.java,
      Period::class.java
   )

   val convertersToRegister: Collection<Converter<*, *>>
      get() {
         val converters = listOf(
            DateToLocalDateTimeConverter,
            LocalDateTimeToDateConverter,
            DateToLocalDateConverter,
            LocalDateToDateConverter,
            DateToLocalTimeConverter,
            LocalTimeToDateConverter,
            DateToInstantConverter,
            InstantToDateConverter,
            LocalDateTimeToInstantConverter,
            InstantToLocalDateTimeConverter,
            ZoneIdToStringConverter,
            StringToZoneIdConverter,
            DurationToStringConverter,
            StringToDurationConverter,
            PeriodToStringConverter,
            StringToPeriodConverter,
            StringToLocalDateConverter,
            StringToLocalDateTimeConverter,
            StringToInstantConverter
         )
         return converters
      }


   object DateToLocalDateTimeConverter : Converter<Date, LocalDateTime> {


      @NonNull
      override fun convert(source: Date): LocalDateTime {
         return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault())
      }
   }


   object LocalDateTimeToDateConverter :
      Converter<LocalDateTime, Date> {


      @NonNull
      override fun convert(source: LocalDateTime): Date {
         return Date.from(source.atZone(ZoneId.systemDefault()).toInstant())
      }
   }


   object DateToLocalDateConverter : Converter<Date, LocalDate> {


      @NonNull
      override fun convert(source: Date): LocalDate {
         return LocalDateTime.ofInstant(Instant.ofEpochMilli(source.time), ZoneId.systemDefault()).toLocalDate()
      }
   }


   object LocalDateToDateConverter :
      Converter<LocalDate, Date> {


      @NonNull
      override fun convert(source: LocalDate): Date {
         return Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant())
      }
   }


   object DateToLocalTimeConverter : Converter<Date, LocalTime> {


      @NonNull
      override fun convert(source: Date): LocalTime {
         return LocalDateTime.ofInstant(Instant.ofEpochMilli(source.time), ZoneId.systemDefault()).toLocalTime()
      }
   }


   object LocalTimeToDateConverter :
      Converter<LocalTime, Date> {


      @NonNull
      override fun convert(source: LocalTime): Date {
         return Date.from(source.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant())
      }
   }


   object DateToInstantConverter : Converter<Date, Instant> {


      @NonNull
      override fun convert(source: Date): Instant {
         return source.toInstant()
      }
   }


   object InstantToDateConverter : Converter<Instant, Date> {


      @NonNull
      override fun convert(source: Instant): Date {
         return Date.from(source)
      }
   }


   object LocalDateTimeToInstantConverter :
      Converter<LocalDateTime, Instant> {


      @NonNull
      override fun convert(source: LocalDateTime): Instant {
         return source.atZone(ZoneId.systemDefault()).toInstant()
      }
   }


   object InstantToLocalDateTimeConverter :
      Converter<Instant, LocalDateTime> {


      @NonNull
      override fun convert(source: Instant): LocalDateTime {
         return LocalDateTime.ofInstant(source, ZoneId.systemDefault())
      }
   }


   object ZoneIdToStringConverter : Converter<ZoneId, String> {


      @NonNull
      override fun convert(source: ZoneId): String {
         return source.toString()
      }
   }


   object StringToZoneIdConverter : Converter<String, ZoneId> {


      @NonNull
      override fun convert(source: String): ZoneId {
         return ZoneId.of(source)
      }
   }


   object DurationToStringConverter :
      Converter<Duration, String> {


      @NonNull
      override fun convert(duration: Duration): String {
         return duration.toString()
      }
   }


   object StringToDurationConverter : Converter<String, Duration> {


      @NonNull
      override fun convert(s: String): Duration {
         return Duration.parse(s)
      }
   }


   object PeriodToStringConverter : Converter<Period, String> {


      @NonNull
      override fun convert(period: Period): String {
         return period.toString()
      }
   }


   object StringToPeriodConverter : Converter<String, Period> {


      @NonNull
      override fun convert(s: String): Period {
         return Period.parse(s)
      }
   }


   object StringToLocalDateConverter : Converter<String, LocalDate> {


      @NonNull
      override fun convert(source: String): LocalDate {
         return LocalDate.parse(source, DateTimeFormatter.ISO_DATE)
      }
   }


   object StringToLocalDateTimeConverter :
      Converter<String, LocalDateTime> {


      @NonNull
      override fun convert(source: String): LocalDateTime {
         return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME)
      }
   }


   object StringToInstantConverter : Converter<String, Instant> {


      @NonNull
      override fun convert(source: String): Instant {
         return Instant.parse(source)
      }
   }
}
