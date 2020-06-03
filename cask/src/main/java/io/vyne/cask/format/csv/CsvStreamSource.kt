package io.vyne.cask.format.csv

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.timed
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.apache.commons.csv.CSVFormat
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class CsvStreamSource(val input: Flux<InputStream>,
                      val type: VersionedType,
                      val schema: Schema,
                      val readCacheDirectory: Path,
                      val bytesPerColumn: Int = 1000,
                      val csvFormat: CSVFormat = CSVFormat.DEFAULT,
                      val nullValues: Set<String> = emptySet()) : StreamSource {

   val cachePath: Path by lazy {
      Files.createFile(readCacheDirectory.resolve(type.versionedName))
   }
   private val writer = CsvBinaryWriter(bytesPerColumn, csvFormat)
   private val mapper =  CsvStreamMapper(type.type, schema)

   override val stream: Flux<InstanceAttributeSet>
      get() {
         return timed("CsvStreamSource.get", true, TimeUnit.MILLISECONDS) {
            input
               .map { writer.convert(it, cachePath) }
               .concatMap { it }
               .map { mapper.map(it, nullValues) }
         }
      }
}
