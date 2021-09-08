package io.vyne.models.csv

import org.apache.commons.csv.CSVFormat

object CsvFormatFactory  {
   fun default():CSVFormat {
      return fromParameters(CsvIngestionParameters())
   }
   fun fromParameters(parameters:CsvIngestionParameters):CSVFormat {
      val format = CSVFormat.DEFAULT
         .withTrailingDelimiter(parameters.containsTrailingDelimiters)
         .withIgnoreEmptyLines()
         .withIgnoreSurroundingSpaces()
         .withDelimiter(parameters.delimiter)
      if (parameters.firstRecordAsHeader) {
         return format
            .withFirstRecordAsHeader()
            .withAllowMissingColumnNames()
            .withAllowDuplicateHeaderNames()
      }
      return format
   }
}
