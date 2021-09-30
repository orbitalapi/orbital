package io.vyne.pipelines.jet.sink.http

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.sink.PipelineSinkBuilder
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import javax.annotation.Resource

class TaxiOperationSinkBuilder : PipelineSinkBuilder<TaxiOperationOutputSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.output is TaxiOperationOutputSpec
   }

   override fun getRequiredType(pipelineSpec: PipelineSpec<*, TaxiOperationOutputSpec>, schema: Schema): QualifiedName {
      val (service, operation) = schema.operation(pipelineSpec.output.operationName.fqn())
      if (operation.parameters.size > 1) {
         error("TaxiOperationSinkBuilder currently only supports single-parameter operations.  ${operation.qualifiedName} requires ${operation.parameters.size}")
      }
      val inputPayloadParam = operation.parameters.first()
      val inputPayloadType = inputPayloadParam.type
      return inputPayloadType.qualifiedName
   }


   override fun build(pipelineSpec: PipelineSpec<*, TaxiOperationOutputSpec>): Sink<MessageContentProvider> {
      return SinkBuilder
         .sinkBuilder("operation-invocation-sink") { context ->
            TaxiOperationSinkContext(
               context.logger(),
               pipelineSpec
            )
         }
         .receiveFn { context: TaxiOperationSinkContext, message: MessageContentProvider ->
            val vyne = context.vyneProvider.createVyne()
            val schema = vyne.schema
            val (service, operation) = schema.operation(context.outputSpec.operationName.fqn())
            val inputPayloadParam = operation.parameters.first()
            val inputPayloadType = inputPayloadParam.type
            val input = TypedInstance.from(inputPayloadType, message.asString(ConsoleLogger), schema)

            val serviceCallResult = runBlocking {
               context.logger.info("Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()}")
               try {
                  val result = vyne.from(input)
                     .invokeOperation(
                        service,
                        operation,
                        setOf(input),
                        listOf(inputPayloadParam to input)
                     )
                     .toList()
                  context.logger.info("Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()} completed")
                  result
               } catch (e: Exception) {
                  context.logger.severe(
                     "Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()} threw exception ${e.message}",
                     e
                  )
                  null
               }
            }
            serviceCallResult
         }
         .build()
   }


}

@SpringAware
class TaxiOperationSinkContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<*, TaxiOperationOutputSpec>
) {
   val outputSpec: TaxiOperationOutputSpec = pipelineSpec.output

   @Resource
   lateinit var vyneProvider: VyneProvider
}
