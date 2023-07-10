package io.vyne.pipelines.jet.source

import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.CsvRecordContentProvider
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.schemas.Type
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

/**
 * WE have a few places where text might be read either as CSV, or plain text.
 *
 * Encapsulating the logic here.
 * However: This is really tech debt.
 *
 * The parsing infrastructure should know enough about how to read this,
 * through the CSVFormat / format detector.
 */
object TextFormatUtils {

   val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))
   fun getCsvFormat(inputType: Type): Pair<CsvFormatSpecAnnotation?, CSVFormat?> {
      val csvModelFormatAnnotation = formatDetector.getFormatType(inputType)
         ?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }

      val csvFormat =
         csvModelFormatAnnotation?.let { CsvFormatFactory.fromParameters(csvModelFormatAnnotation.ingestionParameters) }
      return Pair(csvModelFormatAnnotation, csvFormat)
   }

   fun readAsTextOrCsv(inputStream: InputStream, csvFormat: CSVFormat?): Stream<out Serializable> {
      return if (csvFormat != null) {
         csvFormat.parse(inputStream.bufferedReader()).stream()
      } else {
         val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
         reader.lines()
      }
   }

   fun csvOrStringContentProvider(line: Any, modelFormat: CsvFormatSpecAnnotation?): MessageContentProvider {
      return if (line is CSVRecord) {
         require(modelFormat != null)
         CsvRecordContentProvider(line, modelFormat.ingestionParameters.nullValue)
      } else {
         StringContentProvider(line as String)
      }
   }

}