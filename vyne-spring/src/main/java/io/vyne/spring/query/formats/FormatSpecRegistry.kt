package io.vyne.spring.query.formats

import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.ModelFormatSpec
import io.vyne.protobuf.ProtobufFormatSpec
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
