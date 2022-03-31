package io.vyne.pipelines.jet.source.http.poll

import com.hazelcast.jet.core.metrics.Metrics
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.StreamSource
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedCollection
import io.vyne.models.TypedNull
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineVariableKeys
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.Clock
import java.time.Instant
import java.util.Date
import javax.annotation.PostConstruct
import javax.annotation.Resource

@Component
class PollingTaxiOperationSourceBuilder : PipelineSourceBuilder<PollingTaxiOperationInputSpec> {
   companion object {
      const val NEXT_SCHEDULED_TIME_KEY = "next-scheduled-time"
   }
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is PollingTaxiOperationInputSpec
   }

   override fun build(pipelineSpec: PipelineSpec<PollingTaxiOperationInputSpec, *>): StreamSource<MessageContentProvider> {
      return SourceBuilder.timestampedStream("taxi-operation-poll") { context ->
         PollingTaxiOperationSourceContext(context.logger(), pipelineSpec)
      }
         .fillBufferFn { context: PollingTaxiOperationSourceContext, buffer: SourceBuilder.TimestampedSourceBuffer<MessageContentProvider> ->
            val schedule = CronSequenceGenerator(context.inputSpec.pollSchedule)
            val nextScheduledRunTime = schedule.next(context.lastRunTime)
            Metrics.metric(NEXT_SCHEDULED_TIME_KEY).set(nextScheduledRunTime.toEpochMilli())
            if (nextScheduledRunTime.isAfter(context.clock.instant())) {
               // Not scheduled to do any work yet, so bail.
               return@fillBufferFn
            }
            context.lastRunTime = context.clock.instant()
            context.logger.info("Updating lastRunTime.  Next scheduled to run at ${schedule.next(context.lastRunTime)}")
            PollingTaxiOperationSource(context, buffer).doWork()
         }
         .build()
   }

   override fun getEmittedType(
       pipelineSpec: PipelineSpec<PollingTaxiOperationInputSpec, *>,
       schema: Schema
   ): QualifiedName {
      val (service, operation) = schema.operation(pipelineSpec.input.operationName.fqn())
      return operation.returnType.name
   }
}

internal fun CronSequenceGenerator.next(lastRunTime: Instant): Instant {
   return this.next(Date.from(lastRunTime)).toInstant()
}

private class PollingTaxiOperationSource(
   val context: PollingTaxiOperationSourceContext,
   val buffer: SourceBuilder.TimestampedSourceBuffer<MessageContentProvider>
) : Serializable {
   private val logger = context.logger
   private val inputSpec = context.inputSpec
   private val variableProvider = context.variableProvider.getVariableProvider(context.pipelineSpec.name)
   private val clock = context.clock
   fun doWork() {
      val vyne = context.vyneProvider.createVyne()
      val schema = vyne.schema
      val (service, operation) = try {
         schema.operation(inputSpec.operationName.fqn())
      } catch (e: Exception) {
         logger.severe("Failed to fetch operation ${inputSpec.operationName} from the schema. Aborting.")
         return
      }

      // Using the invokerService (via vyne) here is long-term a Bad Idea.
      // We're deserializing to a TypedInstance, however we don't know
      // if the downstream conusmers want a TypedInstance, or just the bytes.
      // TypedInstance conversion is lossy for attributes not defined in the
      // schema, which will ultimately cause problems if we're not doing
      // transformations.
      // However, for the current task, that'll do, pig.  That'll do.
      val invocationResult = runBlocking {
         try {
            // Create a Vyne instance using the parameters we've resolved from the spec
            // as our starter facts.
            // This will allow them to become candidates for inputs into parameters of
            // operations
            val resolvedParameters = variableProvider.asTypedInstances(inputSpec.parameterMap, operation, vyne.schema)
            val parameterValues = resolvedParameters.map { it.second }.toSet()
            val vyne = vyne.from(parameterValues)
            // Then call the operation via Vyne
            vyne.invokeOperation(service, operation, parameterValues, resolvedParameters)
               .toList()
         } catch (e: Exception) {
            logger.severe("Exception occurred when invoking operation ${inputSpec.operationName}: ${e.message}", e)
            null
         }
      } ?: return

      // Set the last run time to now.
      variableProvider.set(PipelineVariableKeys.PIPELINE_LAST_RUN_TIME, clock.instant())
      context.logger.info("Poll operation ${operation.qualifiedName.shortDisplayName} returned ${invocationResult.size} results")
      val typedInstance = when {
         invocationResult.isEmpty() -> TypedNull.create(operation.returnType)
         invocationResult.size == 1 -> invocationResult.first()
         else -> TypedCollection.from(invocationResult)
      }

      buffer.add(
         TypedInstanceContentProvider(typedInstance),
         clock.millis()
      )

   }
}

@SpringAware
class PollingTaxiOperationSourceContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<PollingTaxiOperationInputSpec, *>
) {
   val inputSpec: PollingTaxiOperationInputSpec = pipelineSpec.input

   @Resource
   lateinit var vyneProvider: VyneProvider

   @Resource
   lateinit var variableProvider: PipelineAwareVariableProvider

   @Resource
   lateinit var clock: Clock

   private var _lastRunTime: Instant? = null

   // Getter/Setter here to help with serialization challenges and race conditions of spring injecting the clock
   var lastRunTime: Instant
      get() {
         return _lastRunTime!!
      }
      set(value) {
         _lastRunTime = value
      }

   @PostConstruct
   fun init() {
      if (_lastRunTime == null) {
         logger.info("Initializing context for input for pipeline ${pipelineSpec.name}.  Setting lastRunTime to current time of ${clock.instant()}")
         lastRunTime = clock.instant()
      }
   }

}
