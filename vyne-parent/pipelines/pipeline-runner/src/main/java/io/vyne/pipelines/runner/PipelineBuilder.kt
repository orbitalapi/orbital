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
            // TODO : The idea here is that metadata may provide hints as to whether
            // or not we want to deserailize the message.
            // Note, as I type this, that may be redundant, as the input feed
            // has enough hints to decide that, and is the concerete place to
            // express the decision.
            // For now, just deserialize everything.
            message.messageProvider()
         }
         .map { typedInstance ->
            // TODO : Handle failed transformations.
            // Question: Should Pipelines have dead letter or error topics?
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
         disposable,
         input,
         output
      )
   }
}
