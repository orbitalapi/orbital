package io.vyne.models.csv

import io.vyne.models.format.ModelFormatDeserializer
import io.vyne.schemas.Metadata
import org.apache.commons.csv.CSVParser

object CsvFormatDeserializer : ModelFormatDeserializer {
   override fun parseRequired(value: Any, metadata: Metadata): Boolean {
      return value is String
   }

   override fun parse(value: Any, metadata: Metadata): Any {
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      require(value is String)
      val format = CsvFormatFactory.fromParameters(csvAnnotation.ingestionParameters)
      val content = CsvImporterUtil.trimContent(value, csvAnnotation.ingestionParameters.ignoreContentBefore)
      val parsed = CSVParser.parse(content, format)
      val records = parsed.records
         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
      return records
   }

}
