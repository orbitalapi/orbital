package io.vyne.pipelines.runner.transport.http

import io.vyne.models.TypedInstance
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.transport.PipelineOutputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.query.DefaultQueryEngineFactory
import io.vyne.query.QueryContext
import io.vyne.query.QueryProfiler
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.*

@Component
class TaxiOperationOutputBuilder(val invokerService: OperationInvocationService) :
   PipelineOutputTransportBuilder<TaxiOperationOutputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.direction == PipelineDirection.OUTPUT && spec.type == TaxiOperationTransport.TYPE
   }

   override fun build(
      spec: TaxiOperationOutputSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory
   ): PipelineOutputTransport {
      return OperationInvokerPipelineOutput(
         spec, invokerService, logger
      )
   }
}

class OperationInvokerPipelineOutput(
   private val spec: TaxiOperationOutputSpec,
   private val invokerService: OperationInvocationService,
   private val logger: PipelineLogger
) : PipelineOutputTransport {
   override fun write(message: MessageContentProvider, logger: PipelineLogger, schema: Schema) {
      val (service, operation) = schema.operation(spec.operationName.fqn())
      val inputPayloadParam = operation.parameters.first()
      val inputPayloadType = inputPayloadParam.type
      val input = TypedInstance.from(inputPayloadType, message.asString(logger), schema)
      val emptyQueryEngine = DefaultQueryEngineFactory(emptyList(), invokerService).queryEngine(schema)
      val invocationResult = runBlocking {
         emptyQueryEngine.invokeOperation(
            service,
            operation,
            setOf(input),
            QueryContext.from(
               schema,
               emptySet(),
               emptyQueryEngine,
               QueryProfiler(),
               queryId = UUID.randomUUID().toString()
            ),
            listOf(inputPayloadParam to input)
         ).toList()
      }
   }

   override val description: String = "Output to operation ${spec.operationName}"

   override fun type(schema: Schema): Type {
      val (service, operation) = schema.remoteOperation(spec.operationName.fqn())
      require(operation.parameters.size == 1) { "Operation targets currently only support single parameter operations.  ${operation.qualifiedName.fullyQualifiedName} expects ${operation.parameters.size}." }
      return operation.parameters.first().type
   }
}
