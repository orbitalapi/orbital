package io.vyne.cask.ingest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.io.SplittableInputStream
import io.vyne.cask.io.TimeLog
import io.vyne.utils.Benchmark.benchmark
import org.apache.commons.io.IOUtils
import org.junit.Test

class SplittableInputStreamTest {
   @Test
   fun performanceBenchmark() {
      val objectMapper1 = jacksonObjectMapper()
//      benchmark("reading from input stream", 100, 1000) {
//         objectMapper1.readTree(IOUtils.toInputStream(JsonDeserializationTest.source))
//      }
      benchmark("reading from input stream with one reader", 100, 1000) {
         val splittableStream = SplittableInputStream.from(IOUtils.toInputStream(JsonDeserializationTest.source))
         objectMapper1.readTree(splittableStream)
      }
      System.out.println("Total time: ${TimeLog.totalTime}")
   }


}
