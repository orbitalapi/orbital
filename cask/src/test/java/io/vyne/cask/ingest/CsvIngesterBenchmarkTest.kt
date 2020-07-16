package io.vyne.cask.ingest

import arrow.core.getOrElse
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.websocket.CsvIngestionRequest
import io.vyne.schemas.fqn
import io.vyne.spring.LocalResourceSchemaProvider
import io.vyne.utils.Benchmark
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.FileUtils
import org.flywaydb.core.Flyway
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.nio.file.Paths
import java.sql.Time
import java.text.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CsvIngesterBenchmarkTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(0) }

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester

   @Before
   fun setup() {
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:${pg.embeddedPostgres.port}/postgres")
         .username("postgres")
         .build()
      Flyway.configure()
         .dataSource(dataSource)
         .load()
         .migrate()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
   }

   @Ignore
   @Test
   fun canStreamDataToPostgresOnStart() {
      val schema = CoinbaseOrderSchema.schemaV1
      val type = schema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()

      Benchmark.benchmark("ingest to db") { stopwatch ->
         val input: Flux<InputStream> = Flux.just(File(resource).inputStream())
         val pipelineSource = CsvStreamSource(input, type, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
         val pipeline = IngestionStream(
            type,
            TypeDbWrapper(type, schema, pipelineSource.cachePath, null),
            pipelineSource)

         ingester = Ingester(jdbcTemplate, pipeline)
         // Ensure clean before we start
         ingester.destroy()

         ingester.initialize()

         ingester.ingest().collectList().block()
         stopwatch.stop()

         val rowCount = ingester.getRowCount()
         rowCount.should.equal(23695)
         ingester.destroy()
         FileUtils.cleanDirectory(folder.root)
      }
   }

   @Ignore
   @Test
   fun canIngestCsvToCask() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val input: Flux<InputStream> = Flux.just(File(source).inputStream())
      val schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      val ingesterFactory = IngesterFactory(jdbcTemplate)
      val caskDAO: CaskDAO = mock()
      val caskService = CaskService(
         schemaProvider,
         ingesterFactory,
         caskDAO
      )
      val type = caskService.resolveType("OrderWindowSummaryCsv").getOrElse {
         error("Type not found")
      }
      caskService.ingestRequest(
         CsvIngestionRequest(CSVFormat.DEFAULT.withFirstRecordAsHeader(), type, emptySet()),
         input
      ).blockFirst()
   }

   @Test
   fun canIngestCsvWithColumnNames() {
      val schemaV3 = CoinbaseOrderSchema.schemaV3
      val typeV3 = schemaV3.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val input: Flux<InputStream> = Flux.just(File(resource).inputStream())
      val pipelineSource = CsvStreamSource(input, typeV3, schemaV3, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(typeV3, TypeDbWrapper(typeV3, schemaV3, pipelineSource.cachePath, null), pipelineSource)
      val queryView = QueryView(jdbcTemplate)

      ingester = Ingester(jdbcTemplate, pipeline)
      ingester.destroy()
      ingester.initialize()
      ingester.ingest().collectList().block()

      val v3QueryStrategy = queryView.getQueryStrategy(typeV3)
      v3QueryStrategy.should.be.instanceof(TableQuerySpec::class.java)

      val rowCount = ingester.getRowCount()
      rowCount.should.equal(1)

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["symbol"].should.equal("BTCUSD")
      6300.compareTo((result.first()["open"] as BigDecimal).toDouble()).should.equal(0)
      6330.0.compareTo((result.first()["high"] as BigDecimal).toDouble()).should.equal(0)
      6235.2.compareTo((result.first()["close"] as BigDecimal).toDouble()).should.equal(0)

      ingester.destroy()
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canGenerateDeltaTable() {
      val schemaV1 = CoinbaseOrderSchema.schemaV1
      val typeV1 = schemaV1.versionedType("OrderWindowSummary".fqn())
      val schemaV2 = CoinbaseOrderSchema.schemaV2
      val typeV2 = schemaV2.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()
      val input: Flux<InputStream> = Flux.just(File(resource).inputStream())
      val pipelineSource = CsvStreamSource(input, typeV1, schemaV1, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(
         typeV1,
         TypeDbWrapper(typeV1, schemaV1, pipelineSource.cachePath, null),
         pipelineSource)

      val queryView = QueryView(jdbcTemplate)

      ingester = Ingester(jdbcTemplate, pipeline)
      // Ensure clean before we start
      ingester.destroy()
      ingester.initialize()
      ingester.ingest().collectList().block()

      val v1QueryStrategy = queryView.getQueryStrategy(typeV1)
      v1QueryStrategy.should.be.instanceof(TableQuerySpec::class.java)

      val v2QueryStrategy = queryView.getQueryStrategy(typeV2)
      v2QueryStrategy.should.be.instanceof(UpgradeDataSourceSpec::class.java)

      Benchmark.benchmark("Update type") { stopwatch ->
         val strategy = queryView.getQueryStrategy(typeV2)
         val upgradeResult = queryView.prepare(strategy, schemaV2).collectList().block()
         stopwatch.stop()
         queryView.destroy(strategy, schemaV2)
         upgradeResult
      }

      ingester.destroy()
      FileUtils.forceDeleteOnExit(folder.root)// this was failing on windows
   }

   @Test
   fun canIngestWithTimeType() {
      val source = """Entity,Time
1,11:11:11
2,23:11:44
"""
      val schema = CoinbaseOrderSchema.schemaTimeTest
      val timeType = schema.versionedType("TimeTest".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, timeType, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(timeType, TypeDbWrapper(timeType, schema, pipelineSource.cachePath, null), pipelineSource)

      ingester = Ingester(jdbcTemplate, pipeline)
      ingester.destroy()
      ingester.initialize()
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["entry"].should.equal("1")
      (result.first()["time"] as Time).toString().should.equal("11:11:11")

      ingester.destroy()
      FileUtils.cleanDirectory(folder.root)
   }
}
