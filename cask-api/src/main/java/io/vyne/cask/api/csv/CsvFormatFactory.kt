package io.vyne.cask.api.csv

import io.vyne.cask.api.CsvIngestionParameters
import org.apache.commons.csv.CSVFormat

object CsvFormatFactory  {
   fun default():CSVFormat {
      return fromParameters(CsvIngestionParameters())
   }
   fun fromParameters(parameters:CsvIngestionParameters):CSVFormat {
      val format = CSVFormat.DEFAULT
         .withTrailingDelimiter(parameters.containsTrailingDelimiters)
         .withIgnoreEmptyLines()
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
