package io.vyne.models

import io.vyne.utils.log
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor


object RawObjectMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? {
      if (typedInstance.value == null) {
         return typedInstance.value
      }
      return if (typedInstance.type.format != null) {
         applyFormat(typedInstance)
      } else {
         typedInstance.value
      }
   }

   private fun applyFormat(typedInstance: TypedInstance): String? {
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


      return when {
         typedInstance.value is LocalDate && dateTimeFormat != null -> DateTimeFormatter
            .ofPattern(dateTimeFormat)
            .withZone(ZoneId.of("UTC"))
            .format((instant as LocalDate).atStartOfDay())

         typedInstance.value is LocalTime && dateTimeFormat != null -> DateTimeFormatter
            .ofPattern(dateTimeFormat)
            .format((instant as LocalTime))

         else -> DateTimeFormatter
            .ofPattern(typedInstance.type.format!!.first())
            .withZone(ZoneId.of("UTC"))
            .format(instant)
      }
   }

   fun findFormatWith(searchPattern: String, formats: List<String>): String? {
      return formats.firstOrNull { it.contains(searchPattern) }
   }
}

object TypeNamedInstanceMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? = TypeNamedInstance(typedInstance.type.name, typedInstance.value, typedInstance.source)
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

class TypedInstanceConverter(private val mapper: TypedInstanceMapper) {

   private fun unwrapMap(valueMap: Map<String, Any>): Map<String, Any?> {
      return valueMap.map { (entryKey, entryValue) ->
         when (entryValue) {
            is TypedInstance -> entryKey to convert(entryValue)
            else -> entryKey to entryValue
         }
      }.toMap()
   }

   private fun unwrapCollection(valueCollection: Collection<*>): List<Any?> {
      return valueCollection.map { collectionMember ->
         when (collectionMember) {
            is TypedInstance -> convert(collectionMember)
            else -> collectionMember
         }
      }
   }

   fun convert(typedInstance: TypedInstance): Any? {
      val value = typedInstance.value
      return when (typedInstance) {
         is Map<*, *> -> mapper.handleUnwrapped(typedInstance, unwrapMap(value as Map<String, Any>))
         is Collection<*> -> unwrapCollection(value as Collection<*>)
         // TODO : There's likely other types that need unwrapping
         else -> mapper.map(typedInstance)
      }

   }
}
