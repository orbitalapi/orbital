package io.vyne.cask.format.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.cask.ingest.JsonDeserializationTest
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark.benchmark
import org.apache.commons.io.IOUtils
import org.junit.Test
import java.util.*

class JsonStreamSourceTest {
   @Test
   fun benchmarkTest() {

      val schema = TaxiSchema.from(JsonDeserializationTest.taxi)
      val objectMapper = jacksonObjectMapper()
      val type = schema.versionedType("foo.Dummy".fqn())
      benchmark("JsonStreamSource stream benchmark", 100, 5000) {
         val id = UUID.randomUUID().toString()
         val result = JsonStreamSource(
            IOUtils.toInputStream(JsonDeserializationTest.source),
            type,
            schema,
            id,
            objectMapper
         ).sequence().toList()
         true
      }
   }
}
