package io.vyne.cask.ingest

import arrow.core.getOrElse
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.MessageIds
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.cask.query.CaskDAO
import io.vyne.schemas.fqn
import io.vyne.spring.LocalResourceSchemaProvider
import io.vyne.utils.Benchmark
import org.apache.commons.csv.CSVFormat
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths

class CsvIngesterBenchmarkTest : BaseCaskIntegrationTest() {


   lateinit var ingester: Ingester

   @Ignore
   @Test
   fun canStreamDataToPostgresOnStart() {
      val schema = CoinbaseOrderSchema.schemaV1
      val type = schema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()

      Benchmark.benchmark("ingest to db") { stopwatch ->
         val pipelineSource = CsvStreamSource(File(resource).inputStream(), type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(),
         ingestionErrorProcessor = caskIngestionErrorProcessor)
         val pipeline = IngestionStream(
            type,
            TypeDbWrapper(type, schema),
            pipelineSource)

         ingester = IngesterFactory(namedParamJdbcTemplate, caskIngestionErrorProcessor)
            .create(pipeline)

         ingestionEventHandler.onIngestionInitialised(IngestionInitialisedEvent(this, type))
         ingester.ingest()
         stopwatch.stop()

         val rowCount = ingester.getRowCount()
         rowCount.should.equal(23695)
      }
   }

   @Ignore
   @Test
   fun canIngestCsvToCask() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val input = File(source).inputStream()
      val schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      val ingesterFactory = IngesterFactory(namedParamJdbcTemplate, caskIngestionErrorProcessor)
      val caskDAO: CaskDAO = mock()
      val caskService = CaskService(
         schemaProvider,
         ingesterFactory,
         configRepository,
         caskDAO,
         ingestionErrorRepository
      )
      val type = caskService.resolveType("OrderWindowSummaryCsv").getOrElse {
         error("Type not found")
      }
//      caskService.ingestRequest(
//
//         CsvWebsocketRequest(CsvIngestionParameters(firstRecordAsHeader = true), type, caskIngestionErrorProcessor),
//         input
//      ).blockFirst()
   }

   @Test
   fun canIngestCsvWithColumnNames() {
      val schemaV3 = CoinbaseOrderSchema.schemaV3
      val typeV3 = schemaV3.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val pipelineSource = CsvStreamSource(File(resource).inputStream(), typeV3, schemaV3, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(typeV3, TypeDbWrapper(typeV3, schemaV3), pipelineSource)
    //  val queryView = QueryView(jdbcTemplate)

      ingester = IngesterFactory.singleThreaded(namedParamJdbcTemplate, caskIngestionErrorProcessor)
         .create(pipeline)

//      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      ingestionEventHandler.onIngestionInitialised(event = IngestionInitialisedEvent(this, typeV3))
      ingester.ingest()

    //  val v3QueryStrategy = queryView.getQueryStrategy(typeV3)
    //  v3QueryStrategy.should.be.instanceof(TableQuerySpec::class.java)

      val rowCount = ingester.getRowCount()
      rowCount.should.equal(1)

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["symbol"].should.equal("BTCUSD")
      6300.compareTo((result.first()["open"] as BigDecimal).toDouble()).should.equal(0)
      6330.0.compareTo((result.first()["high"] as BigDecimal).toDouble()).should.equal(0)
      6235.2.compareTo((result.first()["close"] as BigDecimal).toDouble()).should.equal(0)
   }


}
