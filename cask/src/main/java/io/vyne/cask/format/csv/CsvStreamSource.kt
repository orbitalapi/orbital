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

class CsvStreamSource(val input: Flux<InputStream>,
                      val type: VersionedType,
                      val schema: Schema,
                      val readCacheDirectory: Path,
                      val bytesPerColumn: Int = 30,
                      val csvFormat: CSVFormat = CSVFormat.DEFAULT) : StreamSource {

   val cachePath: Path by lazy {
      Files.createFile(readCacheDirectory.resolve(type.versionedName))
   }
   override val stream: Flux<InstanceAttributeSet>
      get() {
         val writer = CsvBinaryWriter(bytesPerColumn, csvFormat)
         val mapper = CsvStreamMapper(type.type, schema)
         return input
            .map { writer.convert(it, cachePath) }
            .concatMap { it }
            .map { mapper.map(it) }
      }
}
