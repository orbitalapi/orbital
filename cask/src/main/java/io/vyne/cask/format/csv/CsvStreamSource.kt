package io.vyne.cask.format.csv

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.apache.commons.csv.CSVFormat
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class CsvStreamSource(private val input: Flux<InputStream>,
                      private val versionedType: VersionedType,
                      private val schema: Schema,
                      private val readCacheDirectory: Path,
                      private val bytesPerColumn: Int = 1000,
                      private val csvFormat: CSVFormat = CSVFormat.DEFAULT,
                      private val nullValues: Set<String> = emptySet()) : StreamSource {

   val cachePath: Path by lazy {
      Files.createFile(readCacheDirectory.resolve(versionedType.versionedName))
   }
   private val writer = CsvBinaryWriter(bytesPerColumn, csvFormat)
   private val mapper = CsvStreamMapper(versionedType, schema)

   override val stream: Flux<InstanceAttributeSet>
      get() {
         return input
            .map { writer.convert(it, cachePath) }
            .concatMap { it }
            .map { mapper.map(it, nullValues) }
      }
}
