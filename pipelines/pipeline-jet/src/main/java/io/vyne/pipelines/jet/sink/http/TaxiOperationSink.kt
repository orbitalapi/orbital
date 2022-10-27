package io.vyne.pipelines.jet.sink.http

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import javax.annotation.Resource

@Component
class TaxiOperationSinkBuilder : SingleMessagePipelineSinkBuilder<TaxiOperationOutputSpec> {
   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean {
      return pipelineTransportSpec is TaxiOperationOutputSpec
   }

   override fun getRequiredType(pipelineTransportSpec: TaxiOperationOutputSpec, schema: Schema): QualifiedName {
      val (_, operation) = schema.operation(pipelineTransportSpec.operationName.fqn())
      if (operation.parameters.size > 1) {
         error("TaxiOperationSinkBuilder currently only supports single-parameter operations.  ${operation.qualifiedName} requires ${operation.parameters.size}")
      }
      val inputPayloadParam = operation.parameters.first()
      val inputPayloadType = inputPayloadParam.type
      return inputPayloadType.qualifiedName
   }


   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: TaxiOperationOutputSpec
   ): Sink<MessageContentProvider> {
      return SinkBuilder
         .sinkBuilder("operation-invocation-sink") { context ->
            TaxiOperationSinkContext(
               context.logger(),
               pipelineTransportSpec
            )
         }
         .receiveFn { context: TaxiOperationSinkContext, message: MessageContentProvider ->
            val vyne = context.vyneProvider.createVyne()
            val schema = vyne.schema
            val (service, operation) = schema.operation(context.outputSpec.operationName.fqn())
            val inputPayloadParam = operation.parameters.first()
            val inputPayloadType = inputPayloadParam.type
            val input = TypedInstance.from(inputPayloadType, message.asString(), schema)

            runBlocking {
               context.logger.info("Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()}")
               try {
                  vyne.from(input)
                     .invokeOperation(
                        service,
                        operation,
                        setOf(input),
                        listOf(inputPayloadParam to input)
                     )
                     .toList()
                  context.logger.info("Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()} completed")
               } catch (e: Exception) {
                  context.logger.severe(
                     "Invoking operation ${operation.qualifiedName} with arg ${input.toRawObject()} threw exception ${e.message}",
                     e
                  )
               }
            }
         }
         .build()
   }


}

@SpringAware
class TaxiOperationSinkContext(
   val logger: ILogger,
   val outputSpec: TaxiOperationOutputSpec
) {
   @Resource
   lateinit var vyneProvider: VyneProvider
}
