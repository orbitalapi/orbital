package io.vyne.cask.format.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.apache.commons.csv.CSVFormat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CsvStreamMapperBenchmarkTest {

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

   @Test
   fun `can parse csv with format options and skipping inconsistent records`() {
      val schema = CoinbaseOrderSchema.schemaV1
      val versionedType = schema.versionedType("OrderWindowSummary".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Coinbase_BTCUSD_lines_end_with_comma.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false,
         format = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withAllowMissingColumnNames()
            .withTrailingDelimiter()
            .withIgnoreEmptyLines()
      )
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
   fun `can parse date and time`() {
      val schema = CoinbaseOrderSchema.personSchema
      val versionedType = schema.versionedType("demo.Person".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Person_date_time.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false,
         format = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withAllowMissingColumnNames()
            .withTrailingDelimiter()
            .withIgnoreEmptyLines()
      )
      Benchmark.benchmark("can_ingestAndMapToTypedInstance") {
         val file = folder.newFile()

         val results = writer.convert(File(resource).inputStream(), file.toPath())
            .map { mapper.map(it, logMappingTime = false) }
            .collectList()
            .block()
         log().info("Read ${results.size} instances of ${results.first().type.versionedName}")
      }
   }
}
