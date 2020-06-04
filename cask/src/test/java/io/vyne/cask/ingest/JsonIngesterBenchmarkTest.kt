package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import io.vyne.cask.format.csv.Benchmark
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.schemas.fqn
import org.apache.commons.io.FileUtils
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Flux
import java.io.File


class JsonIngesterBenchmarkTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   @Ignore
   fun canStreamJsonData() {
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

         pipelineSource.stream.blockLast()

         stopwatch.stop()

         FileUtils.cleanDirectory(folder.root)
      }
   }
}

