package io.vyne.cask.format.csv

import io.vyne.cask.api.csv.CsvFormatFactory
import io.vyne.cask.format.byteArrayOfLength
import io.vyne.cask.format.unPad
import io.vyne.utils.log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import reactor.core.publisher.Flux
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * This class pads csv content to a specified width.
 * The intention is to allow fast seeking to specific
 * columns per record whilst upgrading.
 * Currently, we're not using this approach, as we haven't
 * proven that the performance benefits this provides are neccessary.
 */
class CsvBinaryWriter(
   private val bytesPerColumn: Int = 15,
   private val format: CSVFormat = CsvFormatFactory.default(),
   private val shouldLogIndividualWriteTime: Boolean = true) {

   fun convert(input: InputStream, outputPath: Path): Flux<CSVRecord> {
      if (Files.exists(outputPath) && Files.size(outputPath) > 0) {
         error("$outputPath already exists")
      }

      if (!Files.exists(outputPath)) {
         Files.createFile(outputPath)
      }

      return Flux.create<CSVRecord> { emitter ->
         FileOutputStream(outputPath.toFile()).use { outputStream ->
            var header: Header? = null
            val parser = format.parse(input.bufferedReader())
            parser.forEach { record ->
               if (!conformsWithHeader(record, parser)) {
                  logRecordMalformedError(record,parser)
               } else {
                  //timed("CsvBinaryWriter.parse", shouldLogIndividualWriteTime , TimeUnit.NANOSECONDS) { // commenting out as it generates lots of noise in tests
                  if (header == null) {
                     header = writeHeader(outputStream, record)
                  }
                  record.forEach { columnValue ->
                     // TODO : The strategy here is to capture that we've overflowed on a specific column,
                     // and then add the adjusted offsets to a header that we take into account when reading
                     require(columnValue.length <= bytesPerColumn) { "Overflow not yet supported.  '$columnValue' had lenght of ${columnValue.length} which exceeds max column size of ${bytesPerColumn}" }

                     outputStream.write(columnValue.byteArrayOfLength(bytesPerColumn))
                  }
                  emitter.next(record)
               }

            }
            emitter.complete()
         }
      }

   }

   private fun logRecordMalformedError(record: CSVRecord, parser: CSVParser) {
      log().error("Record ${record.recordNumber} has invalid number of columns.  Expected ${parser.headerNames.size} but got ${record.size()}.  Will ignore this record")
   }

   private fun conformsWithHeader(record: CSVRecord, parser: CSVParser): Boolean {
      return if (parser.headerNames == null || parser.headerNames.isEmpty()) {
         true
      } else {
         parser.headerNames.size == record.size()
      }
   }

   private fun writeHeader(outputStream: FileOutputStream, record: CSVRecord): Header {
      val header = Header(record.size(), bytesPerColumn, record.parser.headerMap)
      outputStream.write(header.asBytes())
      return header
   }
}

data class Header(val recordsPerRow: Int, val bytesPerColumn: Int, val columnMap: Map<String, Int>? = null) {
   companion object {
      const val HEADER_SIZE_BYTES = 10000
      fun parse(chars: CharArray): Header {
         return parse(String(chars))
      }

      private fun parse(input: String): Header {
         val (recordsPerRow, bytesPerColumn, columnMapStr) = input.split("|")
         val columnMapping = columnMapStr
            .trim('{', '}')
            .split(",")
            .map { it.split("=") }
            .map { it.first().trim() to it.last().unPad().toInt() }
            .toMap()

         return Header(recordsPerRow.unPad().toInt(), bytesPerColumn.unPad().toInt(), columnMapping)
      }

   }

   fun asBytes(): ByteArray {
      val content = "$recordsPerRow|$bytesPerColumn|${columnMap.toString().trim('{', '}').trim()}"
      return content.byteArrayOfLength(HEADER_SIZE_BYTES)
   }
}
