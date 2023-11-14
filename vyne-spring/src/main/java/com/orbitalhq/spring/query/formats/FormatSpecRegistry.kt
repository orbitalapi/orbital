package com.orbitalhq.spring.query.formats

import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.formats.xml.XmlFormatSpec
import com.orbitalhq.models.format.DefaultFormatRegistry
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.protobuf.ProtobufFormatSpec

// Note: No really good reason this lives in vyne-spring,
// just started here, and became hard to move.
// Implementation moved to DefaultFormatRegistry in core types.

class FormatSpecRegistry(
   // If this becomes mutable for some reason, encapsulate with the mutable version hidden
   formats: List<ModelFormatSpec> = DEFAULT_SPECS
) : FormatRegistry, DefaultFormatRegistry(formats) {
   companion object {
      val DEFAULT_SPECS = listOf(
         CsvFormatSpec,
         XmlFormatSpec,
         ProtobufFormatSpec
      )

      fun default() = FormatSpecRegistry(DEFAULT_SPECS)
   }
}
