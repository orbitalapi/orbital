package io.vyne.pipelines.runner.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.pipelines.GenericPipelineTransportSpec
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import java.io.File

object PipelineTestUtils {
   private val mapper = jacksonObjectMapper()
      .registerModule(PipelineJacksonModule())
   val GENERIC_OUTPUT_SPEC = GenericPipelineTransportSpec(
      "Generic",
      PipelineDirection.OUTPUT,
      emptyMap()
   )
   val GENERIC_INPUT_SPEC = GenericPipelineTransportSpec(
      "Generic",
      PipelineDirection.INPUT,
      emptyMap()
   )

   /**
    * Handy util that both checks pipeline specs are as expected,
    * and stores the results back into src/test/resources, producing
    * living documentation of the sample spec json
    */
   fun compareSerializedSpecAndStoreResult(
      input: PipelineTransportSpec = GENERIC_INPUT_SPEC,
      output: PipelineTransportSpec = GENERIC_OUTPUT_SPEC,
      filename: String? = null
   ) {
      val pipeline = Pipeline(
         "Sample",
         input = PipelineChannel(input),
         output = PipelineChannel(output)
      )

      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(pipeline)
      val fromJson = mapper.readValue<Pipeline>(json)
      pipeline.should.equal(fromJson)

      val actualFilename = filename ?: "${input.type}-to-${output.type}-pipeline.json"

      storeSpecJson(actualFilename, json)
   }
}

// Convenient way to generate the actual taxi expectation
// Can then check if it matches what you wanted, and check it in - giving a
// history of how the generated taxi output has changed over time
fun storeSpecJson(
   outputFile: String,
   specJson: String
) {
   val runningOnCi = System.getenv("CI")?.toBoolean() ?: false
   if (!runningOnCi) {
      File("src/test/resources/pipeline-specs/$outputFile")
         .writeText(specJson, Charsets.UTF_8)
   }
}
