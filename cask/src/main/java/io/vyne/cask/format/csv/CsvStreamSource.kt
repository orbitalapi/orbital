package io.vyne.cask.format.csv

import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import org.apache.commons.csv.CSVFormat
import reactor.core.publisher.Flux
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class CsvStreamSource(val input: URI,
                      val type: VersionedType,
                      val schema: TaxiSchema,
                      val readCacheDirectory: Path,
                      val bytesPerColumn: Int = 30,
                      val csvFormat: CSVFormat = CSVFormat.DEFAULT) : StreamSource {

    val cachePath: Path by lazy {
        Files.createFile(readCacheDirectory.resolve(type.versionedName))
    }
    override val stream: Flux<InstanceAttributeSet>
        get() {
            return Flux.create { emitter ->
                val writer = CsvBinaryWriter(bytesPerColumn, csvFormat)
                val mapper = CsvStreamMapper(type.type, schema)

                writer.convert(File(input).inputStream(), cachePath)
                        .map { mapper.map(it) }
                        .doOnComplete { emitter.complete() }
                        // TODO : This isn't the right way to wire this up.
                        .subscribe { emitter.next(it) }
            }
        }
}
