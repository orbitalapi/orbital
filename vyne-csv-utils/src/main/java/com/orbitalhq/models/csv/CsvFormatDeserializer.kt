package com.orbitalhq.models.csv

import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import org.apache.commons.csv.CSVParser

object CsvFormatDeserializer : ModelFormatDeserializer {
   override fun parseRequired(value: Any, metadata: Metadata): Boolean {
      return value is String
   }

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): Any {
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      require(value is String)
      val format = CsvFormatFactory.fromParameters(csvAnnotation.ingestionParameters)
      val content = CsvImporterUtil.trimContent(value, csvAnnotation.ingestionParameters.ignoreContentBefore)
      val parsed = CSVParser.parse(content, format)
      val records = parsed.records
//         .filter { parsed.headerNames == null || parsed.headerNames.isEmpty() || parsed.headerNames.size == it.size() }
      return records
   }

}
