package com.orbitalhq.formats.csv

import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import org.apache.commons.lang3.StringEscapeUtils
import java.io.Serializable

object CsvAnnotationSpec {
   val NAME = "com.orbitalhq.formats.Csv".fqn()
   val taxi = """
      namespace ${NAME.namespace} {

         annotation ${NAME.name} {
            delimiter : String = ","
            firstRecordAsHeader : Boolean = true
            nullValue : String?
            containsTrailingDelimiters : Boolean = false
            useFieldNamesAsColumnNames: Boolean = false
            withQuote: String = '"'
            recordSeparator: String = "\r\n"
         }

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
   val useFieldNamesAsColumnNames: Boolean = false,
   val withQuote: Char? = '"',
   val recordSeparator: String = "\r\n"
) : Serializable, AnnotationWrapper {
   override fun asAnnotation(schema: TaxiDocument): Annotation {
      TODO("Not yet implemented")
   }

   val ingestionParameters: CsvIngestionParameters = CsvIngestionParameters(
      delimiter,
      firstRecordAsHeader,
      nullValue = nullValue?.let { setOf(it) } ?: emptySet(),
      containsTrailingDelimiters = containsTrailingDelimiters,
      ignoreContentBefore = ignoreContentBefore,
      withQuote = withQuote,
      recordSeparator = recordSeparator
   )


   companion object {
      private val SEPERATOR_REPLACEMENTS = listOf(
         "\\r" to "\r",
         "\\n" to "\n",
         "\\t" to "\t"
      )

      fun from(metadata: Metadata): CsvFormatSpecAnnotation {
         require(metadata.name == CsvAnnotationSpec.NAME) { "Cannot parse ${CsvAnnotationSpec.NAME} from an annotation of type ${metadata.name}" }
         val delimiter = (metadata.params["delimiter"] as String?)?.let { delimiterStr ->
            if (delimiterStr == """\t""") '\t' else delimiterStr.toCharArray()[0]
         } ?: ','

         val withQuote = when (val withQuoteStr = metadata.params["withQuote"] as String?) {
            null -> '"'
            "null" -> null
            else -> withQuoteStr[0]
         }

         val firstRecordAsHeader: Boolean = metadata.params["firstRecordAsHeader"] as Boolean? ?: true
         val nullValue: String? = metadata.params["nullValue"] as String?
         val containsTrailingDelimiters: Boolean = metadata.params["containsTrailingDelimiters"] as Boolean? ?: false
         val ignoreContentBefore = metadata.params["ignoreContentBefore"] as String?
         val useFieldNamesAsColumnNames = metadata.params["useFieldNamesAsColumnNames"] as Boolean? ?: false
         val recordSeparator = (metadata.params["recordSeparator"] as String? ?: "\r\n").let { value ->
            // If the user has provided the string of \r, it's parsed as \\r, (to escape the sequence),
            // but we actually want the raw sequence.
            SEPERATOR_REPLACEMENTS.fold(value) { acc, replacements->
               val (input, replacement) = replacements
               acc.replace(input,replacement)
            }
         }

         return CsvFormatSpecAnnotation(
            delimiter,
            firstRecordAsHeader,
            nullValue,
            containsTrailingDelimiters,
            ignoreContentBefore,
            useFieldNamesAsColumnNames,
            withQuote,
            recordSeparator
         )
      }
   }


}

object CsvFormatSpec : ModelFormatSpec {
   override val annotations: List<QualifiedName> = listOf(CsvAnnotationSpec.NAME)
   override val serializer: CsvFormatSerializer = CsvFormatSerializer
   override val deserializer: CsvFormatDeserializer = CsvFormatDeserializer
}
