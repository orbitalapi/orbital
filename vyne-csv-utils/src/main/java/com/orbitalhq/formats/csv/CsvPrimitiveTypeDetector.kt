package com.orbitalhq.formats.csv

import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.jetbrains.kotlinx.dataframe.DataColumn
import kotlin.reflect.javaType

object CsvPrimitiveTypeDetector {
   @OptIn(ExperimentalStdlibApi::class)
   fun inferTaxiPrimitive(column: DataColumn<*>): Type {
      try {
         // In most scenarios, we can find the type from what the data frame
         // has inferred as the column data type.
         val primitiveType = PrimitiveTypes.getTaxiPrimitive(column.type().javaType as Class<*>)
         return primitiveType
      } catch (e:Exception) {}

      // Fallback...
      return when {
         // Check for boolean-like values
         column.values().all { it == null || isBooleanLike(it) } -> PrimitiveType.BOOLEAN

         // Check for numeric types
         column.values().all { it == null || it is Number } -> {
            when {
               column.values().any { it is Double || it is Float } -> PrimitiveType.DECIMAL
               column.values().all { it == null || it is Int } -> PrimitiveType.INTEGER
               column.values().all { it == null || it is Long } -> PrimitiveType.LONG
               else -> PrimitiveType.DECIMAL
            }
         }

         // Check for temporal types
         column.values().all { it == null || isTemporalType(it) } -> {
            when {
               column.values().any { it.toString().contains("Z") || it.toString().contains("+") } -> PrimitiveType.INSTANT
               column.values().any { it.toString().contains(":") } -> PrimitiveType.DATE_TIME
               else -> PrimitiveType.LOCAL_DATE
            }
         }

         // Default to String
         else -> PrimitiveType.STRING
      }
   }

   private fun isBooleanLike(value: Any?): Boolean {
      if (value == null) return true
      val strValue = value.toString().lowercase()
      return strValue in setOf("true", "false", "yes", "no", "1", "0", "y", "n")
   }

   private fun isTemporalType(value: Any?): Boolean {
      if (value == null) return true
      val str = value.toString()

      // Common date/time patterns
      val patterns = listOf(
         """^\d{4}-\d{2}-\d{2}(T|\s)\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$""", // ISO-like
         """^\d{2}/\d{2}/\d{4}\s\d{2}:\d{2}:\d{2}$""", // US date/time
         """^\d{4}-\d{2}-\d{2}$""" // ISO date
      )

      return patterns.any { it.toRegex().matches(str) }
   }

}
