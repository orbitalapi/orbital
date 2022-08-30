package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.Util
import com.hazelcast.map.IMap
import com.hazelcast.query.Predicates
import io.vyne.pipelines.jet.api.JobStatus
import io.vyne.pipelines.jet.api.PipelineMetrics
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.ScheduledPipelineTransportSpec
import io.vyne.pipelines.jet.badRequest
import io.vyne.pipelines.jet.source.next
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.TimeUnit

private typealias JetJobId = String

@Component
class PipelineManager(
   private val pipelineFactory: PipelineFactory,
   private val jetInstance: JetInstance,
) {

   data class ScheduledPipeline(
      val nextRunTime: Instant,
      val pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>,
      val submittedPipeline: SubmittedPipeline
   ) :
      Serializable

   private val logger = KotlinLogging.logger {}
   private val submittedPipelines: IMap<JetJobId, SubmittedPipeline> =
      jetInstance.hazelcastInstance.getMap("submittedPipelines")

   private val scheduledPipelines: IMap<String, ScheduledPipeline> =
      jetInstance.hazelcastInstance.getMap("scheduledPipelines")

   fun startPipeline(pipelineSpec: PipelineSpec<*, *>): Pair<SubmittedPipeline, Job?> {
      val pipeline = pipelineFactory.createJetPipeline(pipelineSpec)
      logger.info { "Initializing pipeline \"${pipelineSpec.name}\"." }
      return if (pipelineSpec.input is ScheduledPipelineTransportSpec) {
         scheduleJobToBeExecuted(
            pipelineSpec as PipelineSpec<ScheduledPipelineTransportSpec, *>,
            pipeline.toDotString()
         ) to null
      } else {
         val job = jetInstance.newJob(pipeline)
         val submittedPipeline = SubmittedPipeline(
            pipelineSpec.name,
            job.idString,
            pipelineSpec,
            pipeline.toDotString(),
            DotVizUtils.dotVizToGraphNodes(pipeline.toDotString()),
            cancelled = false
         )
         storeSubmittedPipeline(job.idString, submittedPipeline)
         submittedPipeline to job
      }
   }

   private fun scheduleJobToBeExecuted(
      pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>,
      pipelineDotRepresentation: String
   ): SubmittedPipeline {
      val schedule = CronSequenceGenerator(pipelineSpec.input.pollSchedule)
      val nextScheduledRunTime = schedule.next(Instant.now())
      logger.info("The pipeline \"${pipelineSpec.name}\" is next scheduled to run at ${nextScheduledRunTime}.")
      val submittedPipeline = SubmittedPipeline(
         pipelineSpec.name,
         null,
         pipelineSpec,
         pipelineDotRepresentation,
         DotVizUtils.dotVizToGraphNodes(pipelineDotRepresentation),
         cancelled = false
      )
      scheduledPipelines.put(
         pipelineSpec.id,
         ScheduledPipeline(nextScheduledRunTime, pipelineSpec, submittedPipeline)
      )
      return submittedPipeline
   }

   @Scheduled(fixedDelay = 1000)
   fun runScheduledPipelinesIfAny() {
      scheduledPipelines.entries.forEach {
         if (scheduledPipelines.isLocked(it.key)) {
            logger.info("Pipeline \"${it.value.pipelineSpec.name}\" is already locked for running by another instance - skipping it.")
            return@forEach
         }
         val lock = scheduledPipelines.tryLock(it.key, 1, TimeUnit.SECONDS)
         if (!lock) {
            logger.info("Cannot lock the pipeline \"${it.value.pipelineSpec.name}\" for execution failed - skipping it.")
            return@forEach
         }
         if (it.value.nextRunTime.isAfter(Instant.now())) {
            logger.trace("Skipping pipeline \"${it.value.pipelineSpec.name}\" as it is next scheduled to run at ${it.value.nextRunTime}.")
            scheduledPipelines.unlock(it.key)
            return@forEach
         }
         logger.info("A scheduled run of the pipeline \"${it.value.pipelineSpec.name}\" starting.")
         val pipeline = pipelineFactory.createJetPipeline(it.value.pipelineSpec)
         jetInstance.newJob(pipeline)
         scheduleJobToBeExecuted(it.value.pipelineSpec, it.value.submittedPipeline.dotViz)
         scheduledPipelines.unlock(it.key)
      }
   }

   private fun storeSubmittedPipeline(jobId: String, submittedPipeline: SubmittedPipeline) {
      submittedPipelines.put(jobId, submittedPipeline)
   }

   fun getPipelines(): List<RunningPipelineSummary> {
      val runningPipelines = submittedPipelines.entries
         .map { (key, submittedPipeline) ->
            val job = jetInstance.jobs
               .find { it.idString == key } ?: error("The pipeline \"$key\" is not actually running. ")
            val status = pipelineStatus(job, submittedPipeline)
            RunningPipelineSummary(
               submittedPipeline,
               status
            )
         }

      val scheduledPipelines = scheduledPipelines.entries.map { (key, scheduledPipeline) ->
         val status = PipelineStatus(
            key,
            key,
            JobStatus.SCHEDULED,
            Instant.now(),
            PipelineMetrics(emptyList(), emptyList(), emptyList(), emptyList())
         )
         RunningPipelineSummary(
            scheduledPipeline.submittedPipeline,
            status
         )
      }

      return runningPipelines + scheduledPipelines
   }

   private fun pipelineStatus(job: Job, submittedPipeline: SubmittedPipeline): PipelineStatus {
      val status = if (submittedPipeline.cancelled) {
         JobStatus.CANCELLED
      } else {
         JobStatus.valueOf(job.status.name)
      }
      return PipelineStatus(
         job.name ?: "Unnamed job ${job.id}",
         job.idString,
         status,
         Instant.ofEpochMilli(job.submissionTime),
         MetricsHelper.pipelineMetrics(job.metrics)
      )
   }

   fun deletePipeline(pipelineId: String): PipelineStatus {
      if (scheduledPipelines.containsKey(pipelineId)) {
         scheduledPipelines.remove(pipelineId)
         return PipelineStatus(
            pipelineId,
            pipelineId,
            JobStatus.SCHEDULED,
            Instant.now(),
            PipelineMetrics(emptyList(), emptyList(), emptyList(), emptyList())
         )
      } else {
         val submittedPipeline = getSubmittedPipeline(pipelineId)
         val job = getPipelineJob(submittedPipeline)
         job.cancel()
         val cancelledJob = submittedPipeline.copy(cancelled = true)
         storeSubmittedPipeline(job.idString, cancelledJob)
         return pipelineStatus(job, submittedPipeline)
      }
   }

   private fun getSubmittedPipeline(pipelineId: String): SubmittedPipeline {
      val matchingPipelines = submittedPipelines.values(Predicates.sql("pipelineSpecId = '$pipelineId'"))
      return when {
         matchingPipelines.isEmpty() -> badRequest("No pipeline with id $pipelineId")
         matchingPipelines.size == 1 -> matchingPipelines.single()
         else -> error("Found ${matchingPipelines.size} pipeline jobs with pipelineSpec id $pipelineId")
      }
   }

   fun getPipeline(pipelineSpecId: String): RunningPipelineSummary {
      if (scheduledPipelines.containsKey(pipelineSpecId)) {
         return RunningPipelineSummary(
            scheduledPipelines[pipelineSpecId]!!.submittedPipeline,
            PipelineStatus(
               pipelineSpecId,
               pipelineSpecId,
               JobStatus.SCHEDULED,
               Instant.now(),
               PipelineMetrics(emptyList(), emptyList(), emptyList(), emptyList())
            )
         )
      } else {
         val submittedPipeline = getSubmittedPipeline(pipelineSpecId)
         val job = getPipelineJob(submittedPipeline)
         val status = pipelineStatus(job, submittedPipeline)
         return RunningPipelineSummary(
            submittedPipeline,
            status
         )
      }
   }

   private fun getPipelineJob(
      submittedPipeline: SubmittedPipeline,
   ): Job {
      return jetInstance.getJob(Util.idFromString(submittedPipeline.jobId))
         ?: error("Pipeline ${submittedPipeline.pipelineSpecId} exists, but it's associated job ${submittedPipeline.jobId} has gone away")
   }

}

