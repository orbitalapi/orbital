package io.vyne.pipelines.jet.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import org.junit.Test

data class SimpleSpec(
   val name: String,
   val input: PipelineTransportSpec,
   val output: PipelineTransportSpec
)

class PipelineJsonTest {
   @Test
   fun `can read from json`() {
      val jackson = jacksonObjectMapper().registerModule(PipelineJacksonModule())
      val pipelineSpec = jackson.readValue<PipelineSpec<*, *>>(
//      val pipelineSpec = jackson.readValue<SimpleSpec>(
         """{
            "id" : "pipeline-123",
    "name": "test",
    "input": {
        "type": "taxiOperation",
        "direction": "INPUT",
        "operationName": "OrderService@@listOrders",
        "pollSchedule": "*/10 * * * * *",
        "parameterMap": {
            "since": "foo"
        }
    },
    "output": {
        "type": "taxiOperation",
        "direction": "OUTPUT",
        "operationName": "StockService@@submitOrders",
        "schedule": "",
        "parameterMap": {}
    }
}"""
      )
      pipelineSpec.should.equal(PipelineSpec(
         id = "pipeline-123",
         name = "test",
         input = PollingTaxiOperationInputSpec(
            operationName = "OrderService@@listOrders",
            pollSchedule = "*/10 * * * * *",
            parameterMap = mapOf("since" to "foo")
         ),
         output = TaxiOperationOutputSpec(
            operationName = "StockService@@submitOrders",
         )
      ))
   }

   @Test
   fun `can round trip to json`() {
      val spec = PipelineSpec(
         name = "test",
         input = PollingTaxiOperationInputSpec(
            operationName = "OrderService@@listOrders",
            pollSchedule = "*/10 * * * * *",
            parameterMap = mapOf("since" to "foo")
         ),
         output = TaxiOperationOutputSpec(
            operationName = "StockService@@submitOrders",
         )
      )
      val json = jacksonObjectMapper()
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(spec)

      val fromJson = jacksonObjectMapper().readValue<PipelineSpec<*,*>>(json)
      fromJson.should.equal(spec)
   }
}
