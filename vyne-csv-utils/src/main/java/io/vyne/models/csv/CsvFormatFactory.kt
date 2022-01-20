package io.vyne.models.csv

import org.apache.commons.csv.CSVFormat

object CsvFormatFactory {
   fun default(): CSVFormat {
      return fromParameters(CsvIngestionParameters())
   }

   fun fromParameters(parameters: CsvIngestionParameters): CSVFormat {
      val format: CSVFormat = CSVFormat.DEFAULT
         .withTrailingDelimiter(parameters.containsTrailingDelimiters)
         .withIgnoreEmptyLines()
         .withIgnoreSurroundingSpaces()
         .withDelimiter(parameters.delimiter)
         .let { csvFormat ->
            // For some reason we set nullValue up as a Set<String>
            // Not sure why ... will ask Serhat
            if (parameters.nullValue.size == 1) {
               csvFormat.withNullString(parameters.nullValue.first())
            } else {
               csvFormat
            }
         }
         .let { csvFormat ->
            if (parameters.firstRecordAsHeader) {
               csvFormat
                  .withFirstRecordAsHeader()
                  .withAllowMissingColumnNames()
                  .withAllowDuplicateHeaderNames()
            } else {
               csvFormat
            }
         }
      return format
   }
}
