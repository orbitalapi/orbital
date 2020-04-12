package io.vyne.pipelines.runner

import io.vyne.models.TypedInstance
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.spring.VyneProvider
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component

@Component
class PipelineBuilder(val transportFactory: PipelineTransportFactory, val vyneFactory: VyneProvider) {
   fun build(pipeline: Pipeline): PipelineInstance {
      val vyne = vyneFactory.createVyne()
      // Grab the types early, in case they're not present in Vyne
      val inputType = vyne.type(pipeline.input.type)
      val outputType = vyne.type(pipeline.output.type)
      val input = transportFactory.buildInput(pipeline.input.transport)
      val output = transportFactory.buildOutput(pipeline.output.transport)
      val disposable = input.feed
         .map { message ->
            // Naieve first implementation.
            // Need to leverage the efficient reading we've built for vyne-db module

            // TODO : Handle errors and record
            val messageContents = IOUtils.toString(message.inputStream)
            TypedInstance.from(inputType, messageContents, vyne.schema)
         }
         .map { typedInstance ->
            val vyneResult = vyne.query()
               .addFact(typedInstance)
               .build(outputType.name)
            vyneResult.get(outputType.fullyQualifiedName) ?: error("Conversion failed")
         }
         .subscribe { result ->
            output.write(result)
         }

      return PipelineInstance(
         pipeline,
         disposable
      )
   }
}
