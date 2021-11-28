package io.vyne.models.csv

import io.vyne.annotations.AnnotationWrapper
import io.vyne.models.format.ModelFormatDeserializer
import io.vyne.models.format.ModelFormatSerializer
import io.vyne.models.format.ModelFormatSpec
import io.vyne.schemas.Metadata
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object CsvAnnotation {
   val NAME = "io.vyne.formats.Csv".fqn()
   val taxi = """
      annotation io.vyne.formats.Csv {
         delimiter : String
         firstRecordAsHeader : Boolean
         nullValue : String
         containsTrailingDelimiters : Boolean,
         ignoreContentBefore : String
      }
   """.trimIndent()
}

class CsvFormatSpecAnnotation(
   val delimiter: Char = ',',
   val firstRecordAsHeader: Boolean = true,
   val nullValue: String? = null,
   val containsTrailingDelimiters: Boolean = false,
   val ignoreContentBefore: String? = null
) : AnnotationWrapper {
   override fun asAnnotation(schema: TaxiDocument): Annotation {
      TODO("Not yet implemented")
   }

   val ingestionParameters: CsvIngestionParameters = CsvIngestionParameters(
      delimiter,
      firstRecordAsHeader,
      nullValue = nullValue?.let { setOf(it) } ?: emptySet(),
      containsTrailingDelimiters = containsTrailingDelimiters,
      ignoreContentBefore = ignoreContentBefore
   )


   companion object {
      fun from(metadata: Metadata): CsvFormatSpecAnnotation {
         require(metadata.name == CsvAnnotation.NAME) { "Cannot parse ${CsvAnnotation.NAME} from an annotation of type ${metadata.name}" }
         val delimiter = (metadata.params["delimiter"] as String?)?.toCharArray()?.get(0) ?: ','
         val firstRecordAsHeader: Boolean = metadata.params["firstRecordAsHeader"] as Boolean? ?: true
         val nullValue: String? = metadata.params["nullValue"] as String?
         val containsTrailingDelimiters: Boolean = metadata.params["containsTrailingDelimiters"] as Boolean? ?: false
         val ignoreContentBefore = metadata.params["ignoreContentBefore"] as String?
         return CsvFormatSpecAnnotation(
            delimiter,
            firstRecordAsHeader,
            nullValue,
            containsTrailingDelimiters,
            ignoreContentBefore
         )
      }
   }


}

object CsvFormatSpec : ModelFormatSpec {
   override val annotations: List<QualifiedName> = listOf(CsvAnnotation.NAME)
   override val serializer: ModelFormatSerializer = CsvFormatSerializer
   override val deserializer: ModelFormatDeserializer = CsvFormatDeserializer
}
