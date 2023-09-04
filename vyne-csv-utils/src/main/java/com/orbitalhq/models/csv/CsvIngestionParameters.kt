package com.orbitalhq.models.csv

import java.io.Serializable

data class CsvIngestionParameters(
   val delimiter: Char = ',',
   val firstRecordAsHeader: Boolean = true,
   val nullValue: Set<String> = emptySet(),
   val ignoreContentBefore: String? = null,
   val containsTrailingDelimiters: Boolean = false,
   val debug: Boolean = false,
   val withQuote: Char? = '"',
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
}
