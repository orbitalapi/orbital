package io.vyne.models

import io.vyne.utils.log
import lang.taxi.Operator
import lang.taxi.types.isNullOrEmpty
import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor

private object TypeFormatter {
   private val logger = KotlinLogging.logger {}
   val UtcZoneId = ZoneId.of("UTC")
   fun applyFormat(typedInstance: TypedInstance): String? {
      if (typedInstance.value == null) {
         return null
      }
      if (typedInstance.value !is TemporalAccessor) {
         logger.error { "Expected to find a Date (TemporalAccessor) value in type ${typedInstance.type.qualifiedName.shortDisplayName}, but found an instance of ${typedInstance.value!!::class.simpleName}.  Returning as a string, and not applying the format" }
         return typedInstance.value.toString()
      }
      require(typedInstance.value is TemporalAccessor) { "Formatted types only supported on TemporalAccessors currently.  If you're seeing this error, time to do some work!" }
      require(typedInstance is TypedValue) { "Formatted types are only applicable to scalar TypedValues at present"}
      val instant = typedInstance.value as TemporalAccessor
      val dateTimeFormat = findFormatWith("'T'", typedInstance.format?.patterns ?: emptyList())?.let { dateTimeFormat ->
         // Handle down-cast date time times (eg., a Time type that was ingested with a dateTime format)
         if (instant is LocalTime) {
            log().debug("Modifying dateTime format to be suitable for LocalTime")
            dateTimeFormat.split("'T'")[1].replace("'Z'", "")
         } else {
            dateTimeFormat
         }
      }


      return when (typedInstance.value) {
         is LocalDate -> fromLocalDateToString(typedInstance, dateTimeFormat)
         is LocalTime -> fromLocalDateTimeToString(typedInstance, dateTimeFormat)
         else -> {
            val zoneId = zoneId(typedInstance)
            typedInstance.format?.patterns?.firstOrNull()?.let { firstFormat ->
               DateTimeFormatter
                  .ofPattern(firstFormat)
                  .withZone(zoneId)
                  .format(instant)
            } ?: DateTimeFormatter.ISO_INSTANT.withZone(zoneId).format(instant)
         }
      }
   }

   fun fromLocalDateToString(typedInstance: TypedValue, dateTimeFormat: String?): String? {
      return when {
         typedInstance.value is LocalDate && dateTimeFormat != null -> DateTimeFormatter
            .ofPattern(dateTimeFormat)
            .withZone(UtcZoneId)
            .format((typedInstance.value as LocalDate).atStartOfDay())
         //  dateTimeFormat == null  at this point.
         typedInstance.value is LocalDate && typedInstance.format != null && typedInstance.format.definesPattern -> {
            val format = typedInstance.format.patterns.first()
            val formatter = if (format.contains("HH") || format.contains("hh")) {
               DateTimeFormatter.ISO_DATE.withZone(UtcZoneId)

            } else {
               DateTimeFormatterBuilder()
                  .appendPattern(format)
                  .toFormatter()
                  .withZone(UtcZoneId)
            }
            formatter.format(typedInstance.value as LocalDate)
         }
         else -> DateTimeFormatter.ISO_DATE.withZone(UtcZoneId).format(typedInstance.value as LocalDate)
      }
   }

   fun fromLocalDateTimeToString(typedInstance: TypedValue, dateTimeFormat: String?): String? {
      val formatter = when {
         typedInstance.value is LocalTime && dateTimeFormat != null -> DateTimeFormatter.ofPattern(dateTimeFormat)
         typedInstance.value is LocalTime && dateTimeFormat == null && typedInstance.format != null && typedInstance.format.definesPattern -> {
            val format = typedInstance.format!!.patterns.first()
            if (format.contains("yy")) {
               DateTimeFormatter.ISO_TIME.withZone(UtcZoneId)
            } else {
               DateTimeFormatterBuilder()
                  .appendPattern(format)
                  .toFormatter()
                  .withZone(UtcZoneId)
            }

         }
         else -> DateTimeFormatter.ISO_TIME.withZone(UtcZoneId)
      }
      return formatter.format(typedInstance.value as LocalTime)
   }

   fun findFormatWith(searchPattern: String, formats: List<String>): String? {
      return formats.firstOrNull { it.contains(searchPattern) }
   }

   fun zoneId(typedInstance: TypedValue): ZoneId {
      return try {
         typedInstance.format?.utcZoneOffsetInMinutes?.let {
            ZoneOffset.ofTotalSeconds(it * 60).normalized()
         } ?: UtcZoneId
      } catch (e: Exception) {
         log().warn("offset value of ${typedInstance.format?.utcZoneOffsetInMinutes} not corresponds to a valid ZoneId, so using UTC instead, error => ${e.message}")
         UtcZoneId
      }
   }
}

object RawObjectMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? {
      if (typedInstance.value == null) {
         return typedInstance.value
      }
      return if (typedInstance is TypedValue && !typedInstance.format.isNullOrEmpty()) {
         TypeFormatter.applyFormat(typedInstance)
      } else {
         typedInstance.value
      }
   }
}

object TypeNamedInstanceMapper : TypedInstanceMapper {
   fun formatValue(typedInstance: TypedInstance): Any? {
      val type = typedInstance.type
      val formattedValue =
         if ((type.hasFormat || type.offset != null) && typedInstance.value != null && typedInstance.value !is String) {
            // I feel like this is a bad idea, as the typed value will no longer statisfy the type contract
            // This could cause casing exceptions elsewhere.
            TypeFormatter.applyFormat(typedInstance)
         } else {
            typedInstance.value
         }
      return formattedValue
   }

   override fun map(typedInstance: TypedInstance): Any? {
      return TypeNamedInstance(typedInstance.type.name, formatValue(typedInstance), typedInstance.source)
   }

   override fun handleUnwrapped(original: TypedInstance, value: Any?): Any? {
      return when (value) {
         is TypeNamedInstance -> value
         else -> TypeNamedInstance(original.type.name, value, original.source)
      }
   }
}

interface TypedInstanceMapper {
   fun map(typedInstance: TypedInstance): Any?
   fun handleUnwrapped(original: TypedInstance, value: Any?): Any? {
      return value
   }
   fun handleUnwrappedCollection(original:TypedInstance, value:Any?): Any? {
      return value
   }
}

/**
 * Modifies the data source of the TypedInstance to the value provided.
 * Useful mainly in tests.
 */
class DataSourceMutatingMapper(val dataSource: DataSource) : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any {
      return DataSourceUpdater.update(typedInstance, dataSource)
   }

}

class TypedInstanceConverter(private val mapper: TypedInstanceMapper) {

   private fun unwrapMap(
      valueMap: Map<String, Any>,
      collectDataSourcesTo: MutableList<Pair<TypedInstance, DataSource>>? = null
   ): Map<String, Any?> {
      val unwrapped = valueMap.map { (entryKey, entryValue) ->
         val converted = when (entryValue) {
            is TypedInstance -> entryKey to convertAndCollectDataSources(entryValue, collectDataSourcesTo)
            else -> entryKey to entryValue
         }
         converted
      }.toMap()
      return unwrapped
   }

   private fun unwrapCollection(
      valueCollection: Collection<*>,
      collectDataSourcesTo: MutableList<Pair<TypedInstance, DataSource>>? = null
   ): List<Any?> {
      return valueCollection.map { collectionMember ->
         when (collectionMember) {
            is TypedInstance -> convertAndCollectDataSources(collectionMember, collectDataSourcesTo)
            else -> collectionMember
         }
      }
   }

   /**
    * Converts the provided typedInstance (recursively, in the case of a TypedObject or TypedCollection)
    * using the configured mapper.
    */
   fun convert(typedInstance: TypedInstance): Any? {
      return convertAndCollectDataSources(typedInstance, null)
   }

   /**
    * Converts the provided typedInstance (recursively, in the case of a TypedObject or TypedCollection)
    * using the configured mapper.
    *
    * Also, the DataSource of each visited TypedInstance is also collected, and returned, for usage elsewhere.
    */
   fun convertAndCollectDataSources(typedInstance: TypedInstance): Pair<Any?, List<Pair<TypedInstance, DataSource>>> {
      val dataSources = mutableListOf<Pair<TypedInstance, DataSource>>()
      return convertAndCollectDataSources(typedInstance, collectDataSourcesTo = dataSources) to dataSources
   }

   private fun convertAndCollectDataSources(
      typedInstance: TypedInstance,
      collectDataSourcesTo: MutableList<Pair<TypedInstance, DataSource>>?
   ): Any? {
      val value = typedInstance.value
      val converted = when (typedInstance) {
         is Map<*, *> -> {
            val unwrapped = unwrapMap(value as Map<String, Any>, collectDataSourcesTo)
            mapper.handleUnwrapped(typedInstance, unwrapped)
         }
         is Collection<*> -> {
            val unwrapped = unwrapCollection(value as Collection<*>, collectDataSourcesTo)
            mapper.handleUnwrappedCollection(typedInstance,unwrapped)
         }
         // TODO : There's likely other types that need unwrapping
         else -> {
            collectDataSourcesTo?.add(typedInstance to typedInstance.source)
            mapper.map(typedInstance)
         }
      }
      return converted
   }
}

fun Operator.toSql(): String {
   return when(this) {
      Operator.NOT_EQUAL -> "<>"
      Operator.EQUAL -> "="
      else -> this.symbol
   }
}
