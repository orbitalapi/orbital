package io.vyne.pipelines.jet.source.http.poll

import com.hazelcast.jet.core.metrics.Metrics
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.StreamSource
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.next
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import javax.annotation.PostConstruct
import javax.annotation.Resource

@Component
class PollingQuerySourceBuilder : PipelineSourceBuilder<PollingQueryInputSpec> {
   companion object {
      const val NEXT_SCHEDULED_TIME_KEY = "next-scheduled-time"
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is PollingQueryInputSpec
   }

   override fun build(
      pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider> {
      return SourceBuilder.timestampedStream("query-poll") { context ->
         PollingQuerySourceContext(context.logger(), pipelineSpec)
      }
         .fillBufferFn { context: PollingQuerySourceContext, buffer: SourceBuilder.TimestampedSourceBuffer<MessageContentProvider> ->
            val schedule = CronSequenceGenerator(context.inputSpec.pollSchedule)
            val nextScheduledRunTime = schedule.next(context.lastRunTime)
            Metrics.metric(NEXT_SCHEDULED_TIME_KEY).set(nextScheduledRunTime.toEpochMilli())
            if (nextScheduledRunTime.isAfter(context.clock.instant())) {
               // Not scheduled to do any work yet, so bail.
               return@fillBufferFn
            }
            context.lastRunTime = context.clock.instant()
            context.logger.info("Updating lastRunTime. Next scheduled to run at ${schedule.next(context.lastRunTime)}.")

            runBlocking {
               val vyne = context.vyneProvider.createVyne()
               vyne.query(pipelineSpec.input.query)
                  .results
                  .map { TypedInstanceContentProvider(it) }
                  .collect { buffer.add(it) }
            }

         }
         .build()
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>,
      schema: Schema
   ): QualifiedName? {
      return null
   }
}

@SpringAware
class PollingQuerySourceContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>
) {
   val inputSpec: PollingQueryInputSpec = pipelineSpec.input

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
         logger.info("Initializing context for input for pipeline ${pipelineSpec.name}. Setting lastRunTime to current time of ${clock.instant()}. ")
         lastRunTime = clock.instant()
      }
   }

}
