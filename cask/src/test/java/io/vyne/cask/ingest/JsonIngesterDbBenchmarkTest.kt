package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import io.vyne.cask.MessageIds
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration


class JsonIngesterDbBenchmarkTest : BaseCaskIntegrationTest() {

   @Before
   override fun setup() {
      // Don't call super.setup() as we're changing the defaults
      this.doSetup(ingesterBufferSize = 1000, ingesterBufferTimeout = Duration.ofSeconds(1))
   }


   @After
   override fun tearDown() {
      super.tearDown()
   }

   @Test
   fun canStreamDataToPostgresOnStart() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      Benchmark.benchmark("ingest JSON to db", warmup = 10, iterations = 10) { stopwatch ->

         val pipelineSource = JsonStreamSource(
            File(resource).inputStream(),
            versionedType,
            taxiSchema,
            MessageIds.uniqueId(),
            ObjectMapper())

         val pipeline = IngestionStream(
            versionedType,
            TypeDbWrapper(versionedType, taxiSchema),
            pipelineSource)

         val ingester = ingesterFactory.create(pipeline)
         caskDao.dropCaskRecordTable(versionedType)
         caskDao.createCaskRecordTable(versionedType)

         ingester.ingest()
         stopwatch.stop()

//         val rowCount = ingester.getRowCount()
//         rowCount.should.equal(10061)
      }
   }
}

