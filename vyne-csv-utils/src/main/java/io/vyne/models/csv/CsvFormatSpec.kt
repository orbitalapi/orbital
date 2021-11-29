package io.vyne.models.csv

import io.vyne.annotations.AnnotationWrapper
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
   val ignoreContentBefore: String? = null,
   /**
    * Indicates if the field names should be used for column names.
    * By default, only fields with "by column(..) accessors defined are used for column names.
    * This allows consistent serialization/deserialization for model specs.
    * However, sometimes when writing a query to output to a CSV, including all the by column(..)
    * is just noise if the column names already match.
    * Therefore, set useFieldNamesAsColumnNames to control output.
    * Note - currently, this does not work for deserialization.  Therefore, enabling this
    * means that the spec will result in a one-way-only serialization.  This is useful for anonymous types,
    * as query results, but should be avoided on actual models.
    */
   val useFieldNamesAsColumnNames: Boolean = false
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
         val useFieldNamesAsColumnNames = metadata.params["useFieldNamesAsColumnNames"] as Boolean? ?: false
         return CsvFormatSpecAnnotation(
            delimiter,
            firstRecordAsHeader,
            nullValue,
            containsTrailingDelimiters,
            ignoreContentBefore,
            useFieldNamesAsColumnNames,
         )
      }
   }


}

object CsvFormatSpec : ModelFormatSpec {
   override val annotations: List<QualifiedName> = listOf(CsvAnnotation.NAME)
   override val serializer: CsvFormatSerializer = CsvFormatSerializer
   override val deserializer: CsvFormatDeserializer = CsvFormatDeserializer
}
