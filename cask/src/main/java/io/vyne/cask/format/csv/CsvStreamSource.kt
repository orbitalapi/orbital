package io.vyne.cask.format.csv

import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.apache.commons.csv.CSVFormat
import reactor.core.publisher.Flux
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class CsvStreamSource(private val input: Flux<InputStream>,
                      private val versionedType: VersionedType,
                      private val schema: Schema,
                      override val messageId:String,
                      private val ingestionErrorProcessor: CaskIngestionErrorProcessor,
                      private val csvFormat: CSVFormat = CSVFormat.DEFAULT,
                      private val nullValues: Set<String> = emptySet(),
                      private val ignoreContentBefore: String? = null) : StreamSource {

   private val writer = InputStreamToCsvRecordConverter(csvFormat)
   private val mapper = CsvStreamMapper(versionedType, schema)

   override val stream: Flux<InstanceAttributeSet>
      get() {
         val inputWithoutPrologue = input.map { inputStream ->
            if (ignoreContentBefore != null) {
               val markSupportedStream = if (inputStream.markSupported()) {
                  inputStream
               } else {
                  BufferedInputStream(inputStream)
               }
               var readBytes: Long = 0
               val reader = BufferedReader(InputStreamReader(markSupportedStream))
               markSupportedStream.mark(2096)
               var markerHit = false
               while (!markerHit) {

                  val nextLine = reader.readLine()
                  if (nextLine.startsWith(ignoreContentBefore)) {
                     markSupportedStream.reset()
                     markSupportedStream.skip(readBytes)
                     markerHit = true
                  } else {
                     readBytes += nextLine.toByteArray(Charset.defaultCharset()).size + 1
                  }
               }
               markSupportedStream
            } else {
               inputStream
            }
         }


         return inputWithoutPrologue
            .flatMap { inputStream -> writer.convert(inputStream, messageId, ingestionErrorProcessor, versionedType) }
            .map { csvRecord -> mapper.map(csvRecord, nullValues, messageId) }
      }
}
