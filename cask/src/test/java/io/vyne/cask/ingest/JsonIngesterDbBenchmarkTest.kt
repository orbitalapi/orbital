package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.cask.MessageIds
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.junit.After
import org.junit.Test
import reactor.core.publisher.Flux
import java.io.File
import java.time.Duration


class JsonIngesterDbBenchmarkTest : BaseCaskIntegrationTest() {


   lateinit var ingester: Ingester

   @After
   override fun tearDown() {
      super.tearDown()
   }

   @Test
   fun canStreamDataToPostgresOnStart() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      Benchmark.benchmark("ingest JSON to db") { stopwatch ->

         val pipelineSource = JsonStreamSource(
            Flux.just(File(resource).inputStream()),
            versionedType,
            taxiSchema,
            MessageIds.uniqueId(),
            ObjectMapper())

         val pipeline = IngestionStream(
            versionedType,
            TypeDbWrapper(versionedType, taxiSchema),
            pipelineSource)

<<<<<<< HEAD
         ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink(), CaskMutationDispatcher(), SimpleMeterRegistry())
=======
         ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink(), SimpleMeterRegistry())
>>>>>>> release/0.18.x
         caskDao.dropCaskRecordTable(versionedType)
         caskDao.createCaskRecordTable(versionedType)

         ingester.ingest().collectList()
            .doOnError { error ->
               log().error("Error ", error)
            }
            .block(Duration.ofMillis(500))
         stopwatch.stop()

         val rowCount = ingester.getRowCount()
         rowCount.should.equal(10061)
      }
   }
}

