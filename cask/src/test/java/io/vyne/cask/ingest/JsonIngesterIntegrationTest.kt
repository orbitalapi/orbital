package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.File
import java.time.Duration


class JsonIngesterIntegrationTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(6662) }

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
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      Benchmark.benchmark("ingest JSON to db") { stopwatch ->

         val pipelineSource = JsonStreamSource(
            Flux.just(File(resource).inputStream()),
            versionedType,
            taxiSchema,
            folder.root.toPath(),
            ObjectMapper())

         val pipeline = IngestionStream(
            versionedType,
            TypeDbWrapper(versionedType, taxiSchema, pipelineSource.cachePath, null),
            pipelineSource)

         ingester = Ingester(jdbcTemplate, pipeline)
         ingester.destroy()
         ingester.initialize()

         ingester.ingest().collectList()
            .doOnError { error ->
               log().error("Error ", error)
            }
            .block(Duration.ofMillis(500))
         stopwatch.stop()

         val rowCount = ingester.getRowCount()
         rowCount.should.equal(10061)
         FileUtils.cleanDirectory(folder.root)
      }
   }
}

