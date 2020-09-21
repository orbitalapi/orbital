package io.vyne.cask.format.csv

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.cask.MessageIds
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.csv.CsvFormatFactory
import io.vyne.schemas.fqn
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class CsvIngestionTest {


   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `can parse standard csv`() {
      val schema = CoinbaseOrderSchema.schemaV1
      val versionedType = schema.versionedType("OrderWindowSummary".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Coinbase_BTCUSD_10_records.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false)
      val file = folder.newFile()

      val results = writer.convert(File(resource).inputStream(), file.toPath())
         .map { mapper.map(it, logMappingTime = false, messageId = MessageIds.uniqueId()) }
         .collectList()
         .block()
      results.should.have.size(10)
   }

   @Test
   fun `can parse csv with format options and skipping inconsistent records`() {
      val schema = CoinbaseOrderSchema.schemaV1
      val versionedType = schema.versionedType("OrderWindowSummary".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Coinbase_BTCUSD_lines_end_with_comma.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false,
         format = CsvFormatFactory.fromParameters(CsvIngestionParameters(firstRecordAsHeader = true, containsTrailingDelimiters = true))
      )
      val file = folder.newFile()

      val results = writer.convert(File(resource).inputStream(), file.toPath())
         .map { mapper.map(it, logMappingTime = false, messageId = MessageIds.uniqueId()) }
         .collectList()
         .block()
      results.should.have.size(3)
   }

   @Test
   fun `can parse date and time`() {
      val schema = CoinbaseOrderSchema.personSchemaV2
      val versionedType = schema.versionedType("demo.Person".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Person_date_time.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false,
         format = CsvFormatFactory.fromParameters(CsvIngestionParameters(firstRecordAsHeader = true))
      )
      val file = folder.newFile()

      val results = writer.convert(File(resource).inputStream(), file.toPath())
         .map { mapper.map(it, logMappingTime = false, messageId = MessageIds.uniqueId()) }
         .collectList()
         .block()!!
      results.should.have.size(4)
      results.first().attributes["logDate"]!!.value.should.equal(LocalDate.parse("1999-10-10"))
      results.first().attributes["logTime"]!!.value.should.equal(LocalTime.parse("11:12:13.123"))
   }

   @Test
   fun `can ingest values with thousand seperators in quotes`() {
      val schema = CoinbaseOrderSchema.schemaV1
      val versionedType = schema.versionedType("OrderWindowSummary".fqn())
      val mapper = CsvStreamMapper(versionedType, schema)

      val resource = Resources.getResource("Coinbase_BTCUSD_10_records_thousand_seperators.csv").toURI()
      // Ingest it a few times to get an average performance
      val writer = CsvBinaryWriter(bytesPerColumn = 30, shouldLogIndividualWriteTime = false,
         format = CsvFormatFactory.fromParameters(CsvIngestionParameters(firstRecordAsHeader = true, containsTrailingDelimiters = true))
      )
      val file = folder.newFile()

      val results = writer.convert(File(resource).inputStream(), file.toPath())
         .map { mapper.map(it, logMappingTime = false, messageId = MessageIds.uniqueId()) }
         .collectList()
         .block()
      val firstClosePrice = results[0].attributes["close"]?.value as BigDecimal
      firstClosePrice.compareTo(BigDecimal("6330")).should.equal(0)
      results.should.have.size(10)
   }
}
