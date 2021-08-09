package io.vyne.cask.format.csv

import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngestionError
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import reactor.core.publisher.Flux
import java.io.InputStream
import java.time.Instant

// Simpler alternative to CsvBinaryWriter, which omits
// the facny writing-a-padded-string stuff
class InputStreamToCsvRecordConverter(
   private val format: CSVFormat = CsvFormatFactory.default()) {

   fun convert(input: InputStream, messageId: String, ingestionErrorProcessor: CaskIngestionErrorProcessor, versionedType: VersionedType): Flux<CSVRecord> {
      return Flux.create<CSVRecord> { emitter ->
         val parser = format.parse(input.bufferedReader())
         parser.forEach { record ->
            if (!conformsWithHeader(record, parser)) {
               logRecordMalformedError(record, parser, messageId, ingestionErrorProcessor, versionedType)
            } else {
               //timed("CsvBinaryWriter.parse", shouldLogIndividualWriteTime , TimeUnit.NANOSECONDS) { // commenting out as it generates lots of noise in tests
               emitter.next(record)
            }

         }
         emitter.complete()
      }
   }

   private fun logRecordMalformedError(record: CSVRecord, parser: CSVParser, messageId: String, ingestionErrorProcessor: CaskIngestionErrorProcessor, versionedType: VersionedType) {
      val error = "Record ${record.recordNumber} has invalid number of columns.  Expected ${parser.headerNames.size} but got ${record.size()}.  Will ignore this record"
      log().error(error)
      ingestionErrorProcessor.sink().next(IngestionError(
         error = error,
         caskMessageId = messageId,
         fullyQualifiedName = versionedType.fullyQualifiedName,
         insertedAt = Instant.now()))
   }

   private fun conformsWithHeader(record: CSVRecord, parser: CSVParser): Boolean {
      return if (parser.headerNames == null || parser.headerNames.isEmpty()) {
         true
      } else {
         parser.headerNames.size == record.size()
      }
   }
}

