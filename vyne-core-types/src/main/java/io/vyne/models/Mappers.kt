package io.vyne.models

import io.vyne.utils.log
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor

private object TypeFormatter {
   val UtcZoneId = ZoneId.of("UTC")
   fun applyFormat(typedInstance: TypedInstance): String? {
      require(typedInstance.value is TemporalAccessor) { "Formatted types only supported on TemporalAccessors currently.  If you're seeing this error, time to do some work!" }
      val instant = typedInstance.value as TemporalAccessor
      val dateTimeFormat = findFormatWith("'T'", typedInstance.type.format!!)?.let { dateTimeFormat ->
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
            typedInstance.type.format?.firstOrNull()?.let { firstFormat ->
               DateTimeFormatter
                  .ofPattern(firstFormat)
                  .withZone(zoneId)
                  .format(instant)
            } ?: DateTimeFormatter.ISO_INSTANT.withZone(zoneId).format(instant)
         }
      }
   }

   fun fromLocalDateToString(typedInstance: TypedInstance, dateTimeFormat: String?): String? {
      return when {
         typedInstance.value is LocalDate && dateTimeFormat != null -> DateTimeFormatter
            .ofPattern(dateTimeFormat)
            .withZone(UtcZoneId)
            .format((typedInstance.value as LocalDate).atStartOfDay())
         typedInstance.value is LocalDate && dateTimeFormat == null && typedInstance.type.format != null && typedInstance.type.format?.firstOrNull() != null -> {
            val format = typedInstance.type.format!!.first()
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

   fun fromLocalDateTimeToString(typedInstance: TypedInstance, dateTimeFormat: String?): String? {
      val formatter = when {
         typedInstance.value is LocalTime && dateTimeFormat != null -> DateTimeFormatter.ofPattern(dateTimeFormat)
         typedInstance.value is LocalTime && dateTimeFormat == null && typedInstance.type.format != null && typedInstance.type.format?.firstOrNull() != null -> {
            val format = typedInstance.type.format!!.first()
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

   fun zoneId(typedInstance: TypedInstance): ZoneId {
      return try {
         typedInstance.type.offset?.let {
            ZoneOffset.ofTotalSeconds(it * 60).normalized()
         } ?: UtcZoneId
      } catch (e: Exception) {
         log().warn("offset value of ${typedInstance.type.offset} not corresponds to a valid ZoneId, so using UTC instead, error => ${e.message}")
         UtcZoneId
      }
   }
}

object RawObjectMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? {
      if (typedInstance.value == null) {
         return typedInstance.value
      }
      return if (typedInstance.type.format != null) {
         TypeFormatter.applyFormat(typedInstance)
      } else {
         typedInstance.value
      }
   }

}

object TypeNamedInstanceMapper : TypedInstanceMapper {
   private fun formatValue(typedInstance: TypedInstance): Any? {
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
}

/**
 * Modifies the data source of the TypedInstance to the value provided.
 * Useful mainly in tests.
 */
class DataSourceMutatingMapper(val dataSource:DataSource) : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any {
      return when (typedInstance) {
         is TypedValue -> typedInstance.copy(source = dataSource)
         is TypedObject -> typedInstance.copy(source = dataSource)
         is TypedCollection -> typedInstance.copy(source = dataSource)
         is TypedEnumValue -> typedInstance.copy(source = dataSource)
         else -> error("Unhandled type of TypedInstance: ${typedInstance::class.simpleName}")
      }
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
         is Collection<*> -> unwrapCollection(value as Collection<*>, collectDataSourcesTo)
         // TODO : There's likely other types that need unwrapping
         else -> {
            collectDataSourcesTo?.add(typedInstance to typedInstance.source)
            mapper.map(typedInstance)
         }
      }
      return converted
   }
}
