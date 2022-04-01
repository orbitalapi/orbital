package io.vyne.pipelines.runner.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.pipelines.jet.api.transport.GenericPipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import org.assertj.core.api.Assertions.assertThat
import java.io.File


object PipelineTestUtils {
   private val mapper = jacksonObjectMapper()
      .registerModule(PipelineJacksonModule())
   val GENERIC_OUTPUT_SPEC = GenericPipelineTransportSpec(
      "Generic",
      PipelineDirection.OUTPUT,
   )
   val GENERIC_INPUT_SPEC = GenericPipelineTransportSpec(
      "Generic",
      PipelineDirection.INPUT,
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
      val pipeline = PipelineSpec(
         "Sample",
         id = "Pipeline-${input::class.simpleName}-to-${output::class.simpleName}",
         input = input,
         output = output
      )
      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(pipeline)
      val fromJson = mapper.readValue<PipelineSpec<*,*>>(json)
      assertThat(fromJson).usingRecursiveComparison()
         .isEqualTo(pipeline)

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
