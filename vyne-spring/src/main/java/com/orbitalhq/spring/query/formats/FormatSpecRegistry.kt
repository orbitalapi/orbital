package com.orbitalhq.spring.query.formats

import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.protobuf.ProtobufFormatSpec
import org.springframework.stereotype.Component

@Component
class FormatSpecRegistry(
   // If this becomes mutable for some reason, encapsulate with the mutable version hidden
   val formats:List<ModelFormatSpec> = DEFAULT_SPECS
) {

   companion object {
      val DEFAULT_SPECS = listOf(
         CsvFormatSpec,
         ProtobufFormatSpec
      )
      fun default() = FormatSpecRegistry(DEFAULT_SPECS)
   }


}
