package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngestionStream
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleTaxiSchemaProvider
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

@Ignore
class CaskDAOIntegrationTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(0) }

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester
   lateinit var caskDao: CaskDAO

   @Before
   fun setup() {
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:${pg.embeddedPostgres.port}/postgres")
         .username("postgres")
         .build()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(CoinbaseJsonOrderSchema.sourceV1))
   }

   @After
   fun tearDown() {
      ingester.destroy()
   }

   @Test
   fun canQueryIngestedDataFromDatabase() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      Benchmark.benchmark("ingest JSON to db and query") { stopwatch ->

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

         caskDao.findBy(versionedType, "symbol", "BTCUSD").size.should.equal(10061)
         caskDao.findBy(versionedType, "open", "6300").size.should.equal(7)
         caskDao.findBy(versionedType, "close", "6330").size.should.equal(9689)
         caskDao.findBy(versionedType, "orderDate", "2020-03-19").size.should.equal(10061)
         FileUtils.cleanDirectory(folder.root)
      }
   }
}
