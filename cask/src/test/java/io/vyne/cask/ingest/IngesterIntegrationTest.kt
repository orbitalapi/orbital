package io.vyne.cask.ingest

import arrow.core.getOrElse
import com.google.common.io.Resources
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.websocket.CsvIngestionRequest
import io.vyne.schemas.fqn
import io.vyne.spring.LocalResourceSchemaProvider
import io.vyne.utils.Benchmark
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream
import java.nio.file.Paths

@Ignore
class IngesterIntegrationTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var pg: EmbeddedPostgres
   // Atm there is no way to override dbname/username/pwd

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester

   @Before
   fun setup() {
      pg = EmbeddedPostgres.builder().setPort(6660).start()
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:6660/postgres")
         .username("postgres")
         .build()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)

   }

   @After
   fun tearDown() {
      pg.close()
   }

   @Test
   @Ignore
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
         FileUtils.cleanDirectory(folder.root);
      }
   }

   @Test
   fun canIngestCsvToCask() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val input: Flux<InputStream> = Flux.just(File(source).inputStream())
      val schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      val ingesterFactory = IngesterFactory(jdbcTemplate)
      val caskService = CaskService(
         schemaProvider,
         ingesterFactory
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
   @Ignore
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
//        val rowCount = ingester.getRowCount()
//        rowCount.should.equal(23695)
      ingester.destroy()
      FileUtils.forceDeleteOnExit(folder.root)// this was failing on windows
//        }
   }
}
