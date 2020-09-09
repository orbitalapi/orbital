package io.vyne.cask.format.csv

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
import java.nio.file.Files
import java.nio.file.Path

class CsvStreamSource(private val input: Flux<InputStream>,
                      private val versionedType: VersionedType,
                      private val schema: Schema,
                      private val readCacheDirectory: Path,
                      private val bytesPerColumn: Int = 1000,
                      private val csvFormat: CSVFormat = CSVFormat.DEFAULT,
                      private val nullValues: Set<String> = emptySet(),
                      private val ignoreContentBefore: String? = null) : StreamSource {
// TODO : Need to re-implement this...

   //     applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, request.versionedType))
//            var headerOffset = 0
//            val containsHeader = request.params.getParam("firstRowAsHeader").orElse(false) as Boolean
//            val firstColumn = request.params.getParam("columnOne")
//            val secondColumn = request.params.getParam("columnTwo")
//            val hasHeader = containsHeader || (!firstColumn.isNullOrEmpty() && !secondColumn.isNullOrEmpty())
//
//            if (hasHeader) {
//
//               if (!firstColumn.isNullOrEmpty() && !secondColumn.isNullOrEmpty()) {
//                  headerOffset = input.indexOf("$firstColumn,$secondColumn").orElse(0)
//               }
//            }
//
//            val ingestionInput = if (headerOffset > 0) {
//               Flux.just(input.removeRange(0 until headerOffset).byteInputStream() as InputStream)
//            } else {
//               Flux.just(input.byteInputStream() as InputStream)
//            }
   val cachePath: Path by lazy {
      Files.createFile(readCacheDirectory.resolve(versionedType.versionedName))
   }
   private val writer = CsvBinaryWriter(bytesPerColumn, csvFormat)
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
            .map { writer.convert(it, cachePath) }
            .concatMap { it }
            .map { mapper.map(it, nullValues) }
      }
}
