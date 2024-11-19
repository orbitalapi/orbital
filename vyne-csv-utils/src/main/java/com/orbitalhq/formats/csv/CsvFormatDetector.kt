package com.orbitalhq.formats.csv

object CsvFormatDetector {
   fun detectFormat(csv: String): CsvIngestionParameters {
      val lines = csv.lines()
      // Detect record separator (usually \n or \r\n)
      val recordSeparator = detectRecordSeparator(csv)
      val (prelude,dataLines) = detectPrelude(lines, recordSeparator)

      // Detect delimiter
      val (delimiter, delimiterConfidence) = detectDelimiter(dataLines)

      // Detect quote character
      val quote = detectQuoteCharacter(dataLines, delimiter)

      // Detect if first line is headers
      val (hasHeaders, headerConfidence) = detectHeaders(dataLines, delimiter)

      // Detect null values
      val nullValue = detectNullValue(dataLines, delimiter)

      // Detect comment marker
      val commentMarker = detectCommentMarker(lines)

      return CsvIngestionParameters(
         delimiter,
         hasHeaders,
         setOfNotNull(nullValue),
         prelude,
         quote = quote,
         recordSeparator = recordSeparator,
      )
      TODO()
   }



   private fun detectPrelude(lines: List<String>, recordSeparator: String): Pair<String?, List<String>> {
      var preludeCount = 0
      // Skip empty lines at start
      while (preludeCount < lines.size && lines[preludeCount].isBlank()) {
         preludeCount++
      }

      // Look for metadata-like lines (key: value pairs)
      while (preludeCount < lines.size &&
         (lines[preludeCount].contains(":") ||
            lines[preludeCount].startsWith("#") ||
            lines[preludeCount].startsWith("//") ||
            lines[preludeCount].isBlank())) {
         preludeCount++
      }

      return if (preludeCount == 0) {
         null to lines
      } else {
         lines.subList(0, preludeCount).joinToString(recordSeparator) to lines.drop(preludeCount)
      }


   }


   private fun detectDelimiter(lines: List<String>): Pair<Char, Double> {
      val commonDelimiters = listOf(',', '|', '\t', ';')
      val counts = mutableMapOf<Char, Int>()
      var maxCount = 0
      var bestDelimiter = ','

      // Count occurrences of each delimiter
      lines.take(10).forEach { line ->
         commonDelimiters.forEach { delimiter ->
            val count = line.count { it == delimiter }
            if (count > 0 && count == line.count { it == delimiter }) {
               counts[delimiter] = (counts[delimiter] ?: 0) + 1
               if (counts[delimiter]!! > maxCount) {
                  maxCount = counts[delimiter]!!
                  bestDelimiter = delimiter
               }
            }
         }
      }

      // Calculate confidence based on consistency
      val confidence = counts[bestDelimiter]?.toDouble()?.div(lines.size.toDouble()) ?: 0.0

      return bestDelimiter to confidence
   }

   private fun detectQuoteCharacter(lines: List<String>, delimiter: Char): Char? {
      val commonQuotes = listOf('"', '\'')

      for (quote in commonQuotes) {
         // Check if quote character appears in pairs and contains delimiters
         val hasValidQuotes = lines.take(10).any { line ->
            val quotes = line.count { it == quote }
            quotes % 2 == 0 && quotes > 0 &&
               line.contains(delimiter)
         }
         if (hasValidQuotes) return quote
      }
      return null
   }

   private fun detectHeaders(lines: List<String>, delimiter: Char): Pair<Boolean, Double> {
      if (lines.size < 2) return false to 0.0

      val firstLine = lines.first().split(delimiter)
      val secondLine = lines[1].split(delimiter)

      var confidence = 0.0
      var headerIndicators = 0

      // Check if number of columns is consistent
      if (firstLine.size == secondLine.size) {
         headerIndicators++

         // Check if headers look like column names (no numeric values)
         if (firstLine.none { it.trim().toDoubleOrNull() != null }) {
            headerIndicators++
         }

         // Check if data types differ between header and data
         val headerTypes = firstLine.map { inferSimpleType(it) }
         val dataTypes = secondLine.map { inferSimpleType(it) }
         if (headerTypes != dataTypes) {
            headerIndicators++
         }

         // Check for common header patterns (ID, Name, etc.)
         val commonHeaderPatterns = setOf("id", "name", "date", "code", "type", "status")
         if (firstLine.any { header ->
               commonHeaderPatterns.any { pattern ->
                  header.trim().lowercase().contains(pattern)
               }
            }) {
            headerIndicators++
         }
      }

      confidence = headerIndicators / 4.0
      return (confidence > 0.5) to confidence
   }

   private fun inferSimpleType(value: String): String {
      return when {
         value.trim().toDoubleOrNull() != null -> "number"
         value.trim().toBooleanStrictOrNull() != null -> "boolean"
         else -> "string"
      }
   }

   private fun detectNullValue(lines: List<String>, delimiter: Char): String? {
      val commonNullValues = setOf("NULL", "null", "NA", "N/A", "", "\\N")
      val valueFrequency = mutableMapOf<String, Int>()

      lines.take(100).forEach { line ->
         line.split(delimiter).forEach { value ->
            val trimmedValue = value.trim()
            if (commonNullValues.contains(trimmedValue)) {
               valueFrequency[trimmedValue] = valueFrequency.getOrDefault(trimmedValue, 0) + 1
            }
         }
      }

      return valueFrequency.maxByOrNull { it.value }?.key
   }

   private fun detectCommentMarker(lines: List<String>): String? {
      val commonCommentMarkers = listOf("#", "//")

      lines.take(10).forEach { line ->
         commonCommentMarkers.forEach { marker ->
            if (line.trimStart().startsWith(marker)) {
               return marker
            }
         }
      }
      return null
   }

   private fun detectRecordSeparator(content: String): String {
      return when {
         content.contains("\r\n") -> "\r\n"
         content.contains("\r") -> "\r"
         else -> "\n"
      }
   }

}
