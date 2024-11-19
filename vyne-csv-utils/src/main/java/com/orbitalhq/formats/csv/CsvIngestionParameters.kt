package com.orbitalhq.formats.csv

import java.io.Serializable

data class CsvIngestionParameters(
   val delimiter: Char = ',',
   val firstRecordAsHeader: Boolean = true,
   val nullValue: Set<String> = emptySet(),
   val preludeText: String? = null,
   val containsTrailingDelimiters: Boolean = false,
   val debug: Boolean = false,
   val quote: Char? = '"',
   val recordSeparator: String = "\r\n"
) : Serializable {
   companion object {
      fun guessRecordSeparator(content: String): String {
         return when {
            content.contains("\r\n") -> "\r\n"
            else -> "\n"
         }
      }
   }

   fun withGuessedRecordSeparator(content: String): CsvIngestionParameters {
      return copy(recordSeparator = guessRecordSeparator(content))
   }
   fun toAnnotation():CsvFormatSpecAnnotation {
      return CsvFormatSpecAnnotation(
         delimiter = delimiter,
         firstRecordAsHeader = firstRecordAsHeader,
         nullValue = nullValue.firstOrNull(),
         ignoreContentBefore = preludeText,
         recordSeparator = recordSeparator,
         quoteChar = quote,
      )
   }
}
