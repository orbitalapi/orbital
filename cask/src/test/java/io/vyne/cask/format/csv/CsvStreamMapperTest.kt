package io.vyne.cask.format.csv

import com.google.common.io.Resources
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CsvStreamMapperTest {

    @Rule
    @JvmField
    val folder = TemporaryFolder()

    @Test
    fun can_ingestAndMapToTypedInstance() {
        val schema = CoinbaseOrderSchema.schemaV1
        val versionedType = schema.versionedType("OrderWindowSummary".fqn())
        val mapper = CsvStreamMapper(versionedType, schema)

        val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()
        // Ingest it a few times to get an average performance
        val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false)
       Benchmark.benchmark("can_ingestAndMapToTypedInstance") {
          val file = folder.newFile()

          val results = writer.convert(File(resource).inputStream(), file.toPath())
             .map { mapper.map(it, logMappingTime = false) }
             .collectList()
             .block()
          log().info("Read ${results.size} instances of ${results.first().type.versionedName}")
       }
    }

    @Test
    fun canReadDelta() {
        val schema = CoinbaseOrderSchema.schemaV1
        val versionedType = schema.versionedType("OrderWindowSummary".fqn())
        val mapper = CsvStreamMapper(versionedType, schema)

        val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()
        // Ingest it a few times to get an average performance
        val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false)
        val binaryCaches = Benchmark.warmup("can_ingestAndMapToTypedInstance") {
           val file = folder.newFile()

           writer.convert(File(resource).inputStream(), file.toPath())
              .map { mapper.map(it, logMappingTime = false) }
              .collectList()
              .block()
           file
        }

        val cacheFile = binaryCaches.last()

       Benchmark.benchmark("ReadCacheValues") {
          CsvBinaryReader().readAllValuesAtColumn(cacheFile.toPath(), setOf(4))
             .collectList()
             .block()
       }


    }

}
