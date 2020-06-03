package io.vyne.cask.ingest

import com.google.common.io.Resources
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.schemas.fqn
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

@Ignore
class IngesterIntegrationTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(6661) }
   // Atm there is no way to override dbname/username/pwd

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester

   @Before
   fun setup() {
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:6660/postgres")
         .username("postgres")
         .build()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
   }

   @After
   fun tearDown() {
      ingester.destroy()
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
