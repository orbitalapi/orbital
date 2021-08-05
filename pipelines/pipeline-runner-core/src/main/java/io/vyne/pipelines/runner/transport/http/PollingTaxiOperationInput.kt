package io.vyne.pipelines.runner.transport.http

import io.vyne.models.TypedCollection
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.pipelines.runner.scheduler.CronSchedule
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.query.DefaultQueryEngineFactory
import io.vyne.query.NoOpQueryProfiler
import io.vyne.query.QueryContext
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class PollingTaxiOperationInputBuilder(
   private val invokerService: OperationInvocationService,
   private val schemaProvider: SchemaProvider,
   private val tickFrequency: Duration = Duration.ofSeconds(1),
   private val clock: Clock = Clock.systemUTC()
) :
   PipelineInputTransportBuilder<PollingTaxiOperationInputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.direction == PipelineDirection.INPUT && spec.type == TaxiOperationTransport.TYPE
   }

   override fun build(
      spec: PollingTaxiOperationInputSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory
   ): PollingTaxiOperationPipelineInput {
      return PollingTaxiOperationPipelineInput(
         spec, invokerService, logger, tickFrequency, clock, schemaProvider
      )
   }
}

class PollingTaxiOperationPipelineInput(
   private val spec: PollingTaxiOperationInputSpec,
   private val invokerService: OperationInvocationService,
   private val logger: PipelineLogger,
   private val tickFrequency: Duration,
   private val clock: Clock,
   private val schemaProvider: SchemaProvider,
) : PipelineInputTransport {
   private val cronSchedule: CronSchedule = CronSchedule(spec.pollSchedule, tickFrequency, clock)
   override val feed: Flux<PipelineInputMessage> = cronSchedule.flux
      .mapNotNull {
         logger.info { "Poll job for pipeline ${spec.operationName} starting" }
         // TODO : Test what happens when the schema changes and the operation isn't there anymore
         val schema = schemaProvider.schema()
         val (service, operation) = try {
            schema.operation(spec.operationName.fqn())
         } catch (e: Exception) {
            logger.error { "Failed to fetch operation ${spec.operationName} from the schema. Aborting." }
            return@mapNotNull null
         }

         val emptyQueryEngine = DefaultQueryEngineFactory(emptyList()).queryEngine(schema)
         val queryContext = QueryContext.from(
            schema,
            emptySet(),
            emptyQueryEngine,
            NoOpQueryProfiler(),
            queryId = UUID.randomUUID().toString()
         )

         // Using the invokerService here is long-term a Bad Idea.
         // We're deserializing to a TypedInstance, however we don't know
         // if the downstream conusmers want a TypedInstance, or just the bytes.
         // TypedInstance conversion is lossy for attributes not defined in the
         // schema, which will ultimately cause problems if we're not doing
         // transformations.
         // However, for the current task, that'll do, pig.  That'll do.
         val invocationResult = runBlocking {
            try {
               invokerService.invokeOperation(service, operation, emptySet(), queryContext)
                  .toList()
            } catch (e: Exception) {
               logger.error(e) { "Exception occurred when invoking operation ${spec.operationName}" }
               null
            }
         } ?: return@mapNotNull null

         PipelineInputMessage(
            Instant.now(),
            emptyMap(),
            TypedInstanceContentProvider(TypedCollection.from(invocationResult))
         )
      }
   override val description: String = "Output to operation ${spec.operationName}"

   override fun type(schema: Schema): Type {
      val (service, operation) = schema.remoteOperation(spec.operationName.fqn())
      require(operation.parameters.size == 1) { "Operation targets currently only support single parameter operations.  ${operation.qualifiedName.fullyQualifiedName} expects ${operation.parameters.size}." }
      return operation.parameters.first().type
   }
}


