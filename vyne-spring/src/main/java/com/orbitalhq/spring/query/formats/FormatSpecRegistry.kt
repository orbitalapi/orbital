package com.orbitalhq.spring.query.formats

import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.formats.xml.XmlFormatSpec
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.protobuf.ProtobufFormatSpec
import com.orbitalhq.schemas.Type
import org.springframework.stereotype.Component

@Component
class FormatSpecRegistry(
   // If this becomes mutable for some reason, encapsulate with the mutable version hidden
   val formats:List<ModelFormatSpec> = DEFAULT_SPECS
) {
   private val formatDetector = FormatDetector(formats)
   companion object {
      val DEFAULT_SPECS = listOf(
         CsvFormatSpec,
         XmlFormatSpec,
         ProtobufFormatSpec
      )
      fun default() = FormatSpecRegistry(DEFAULT_SPECS)
   }

   fun forType(type: Type): ModelFormatSpec? {
      return formatDetector.getFormatType(type)?.second
   }

}
