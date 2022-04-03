package io.vyne.models.csv

import java.io.Serializable

data class CsvIngestionParameters(
   val delimiter: Char = ',',
   val firstRecordAsHeader: Boolean = true,
   val nullValue: Set<String> = emptySet(),
   val ignoreContentBefore: String? = null,
   val containsTrailingDelimiters: Boolean = false,
   val debug: Boolean = false,
   val withQuote: Char? = '"'
): Serializable
