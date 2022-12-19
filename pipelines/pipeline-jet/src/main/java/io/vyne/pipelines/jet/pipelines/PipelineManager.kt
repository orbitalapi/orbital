package io.vyne.pipelines.jet.pipelines

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.Util
import com.hazelcast.jet.config.JobConfig
import com.hazelcast.jet.core.JobNotFoundException
import com.hazelcast.jet.impl.JobRecord
import com.hazelcast.jet.impl.JobResult
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
   private val hazelcastInstance: HazelcastInstance,
) {

   data class ScheduledPipeline(
      val nextRunTime: Instant,
      val pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>,
      val submittedPipeline: SubmittedPipeline,
      val jobId: Long? = null
   ) :
      Serializable

   private val logger = KotlinLogging.logger {}
   private val submittedPipelines: IMap<JetJobId, SubmittedPipeline> =
      hazelcastInstance.getMap("submittedPipelines")

   private val scheduledPipelines: IMap<String, ScheduledPipeline> =
      hazelcastInstance.getMap("scheduledPipelines")

   fun startPipeline(pipelineSpec: PipelineSpec<*, *>): Pair<SubmittedPipeline, Job?> {
      val pipeline = pipelineFactory.createJetPipeline(pipelineSpec)
      logger.info { "Initializing pipeline \"${pipelineSpec.name}\"." }
      return if (pipelineSpec.input is ScheduledPipelineTransportSpec) {
         scheduleJobToBeExecuted(
            pipelineSpec as PipelineSpec<ScheduledPipelineTransportSpec, *>,
            pipeline.toDotString()
         ) to null
      } else {
         val job = hazelcastInstance.jet.newJob(pipeline)
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

   fun triggerScheduledPipeline(pipelineSpecId: String): Boolean {
      if (!scheduledPipelines.tryLock(pipelineSpecId, 100, TimeUnit.MILLISECONDS)) {
         return false
      }

      val isScheduledForNextTrigger: Boolean
      try {
         isScheduledForNextTrigger = scheduledPipelines[pipelineSpecId]?.let { scheduledPipeline ->
            if (scheduledPipeline.jobId == null || isJobTerminated(scheduledPipeline.jobId)) {
               scheduledPipelines.put(
                  pipelineSpecId,
                  scheduledPipeline.copy(nextRunTime = Instant.now().minusMillis(500L))
               )
               logger.info { "The scheduled pipeline with id $pipelineSpecId and name \"${scheduledPipeline.pipelineSpec.name}\" marked for execution." }
               true
            } else {
               false
            }
         } ?: false
      } finally {
         scheduledPipelines.unlock(pipelineSpecId)
      }
      return isScheduledForNextTrigger
   }

   private fun isJobTerminated(jobId: Long): Boolean {
      return try {
         hazelcastInstance.jet.getJob(jobId)?.status?.isTerminal ?: true
      } catch (e: JobNotFoundException) {
         //Looks like previously completed job are removed from internal job repository in jet after a certain period.
         // When that happens jet throws JobNotFoundException.
         true
      }
   }

   private fun scheduleJobToBeExecuted(
      pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>,
      pipelineDotRepresentation: String,
      jobId: Long? = null
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
         ScheduledPipeline(nextScheduledRunTime, pipelineSpec, submittedPipeline, jobId)
      )
      return submittedPipeline
   }

   @Scheduled(fixedDelay = 1000)
   fun runScheduledPipelinesIfAny() {
      if (!hazelcastInstance.lifecycleService.isRunning) {
         logger.warn("Hazelcast is not running. Skipping scheduled pipelines execution.")
         return
      }
      scheduledPipelines.entries.forEach {
         if (scheduledPipelines.isLocked(it.key)) {
            logger.info("Pipeline \"${it.value.pipelineSpec.name}\" is already locked for running by another instance - skipping it.")
            return@forEach
         }
         val lock = scheduledPipelines.tryLock(it.key, 1, TimeUnit.SECONDS)
         if (!lock) {
            logger.info("Cannot lock the pipeline \"${it.value.pipelineSpec.name}\" for execution - skipping it.")
            return@forEach
         }

         if (isExecutionScheduledForLater(it.value)) {
            scheduledPipelines.unlock(it.key)
            return@forEach
         }

         if (shouldPreventConcurrentExecution(it.value)) {
            scheduledPipelines.unlock(it.key)
            return@forEach
         }

         logger.info("A scheduled run of the pipeline \"${it.value.pipelineSpec.name}\" starting.")
         val pipeline = pipelineFactory.createJetPipeline(it.value.pipelineSpec)
         val job = hazelcastInstance.jet.newJob(pipeline)
         scheduleJobToBeExecuted(it.value.pipelineSpec, it.value.submittedPipeline.dotViz, job.id)
         scheduledPipelines.unlock(it.key)
      }
   }

   private fun isExecutionScheduledForLater(scheduledPipeline: ScheduledPipeline): Boolean {
      if (scheduledPipeline.nextRunTime.isAfter(Instant.now())) {
         logger.trace("Skipping pipeline \"${scheduledPipeline.pipelineSpec.name}\" as it is next scheduled to run at ${scheduledPipeline.nextRunTime}.")
         return true
      }
      return false
   }

   private fun shouldPreventConcurrentExecution(scheduledPipeline: ScheduledPipeline): Boolean {
      val previousJobTerminated = scheduledPipeline.jobId?.let { jobId ->
         val jobStatus = hazelcastInstance.jet.getJob(jobId)?.status
         logger.trace { "Status for pipeline ${scheduledPipeline.pipelineSpec.name} $jobId is $jobStatus" }
         jobStatus?.isTerminal
      } ?: true

      if (!previousJobTerminated && scheduledPipeline.pipelineSpec.input.preventConcurrentExecution) {
         logger.trace("Skipping pipeline \"${scheduledPipeline.pipelineSpec.name}\" as it is input spec set as fixedDelay, and there is an active job ${scheduledPipeline.jobId}.")
         return true
      }
      return false
   }

   private fun storeSubmittedPipeline(jobId: String, submittedPipeline: SubmittedPipeline) {
      submittedPipelines.put(jobId, submittedPipeline)
   }

   fun getPipelines(): List<RunningPipelineSummary> {
      val runningPipelines = submittedPipelines.entries
         .map { (key, submittedPipeline) ->
            val job = hazelcastInstance.jet.jobs
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

   fun getHazelcastPipelineStatus(pipelineSpecId: String): com.hazelcast.jet.core.JobStatus? {
      return scheduledPipelines[pipelineSpecId]?.let { scheduledPipeline ->
         scheduledPipeline.jobId?.let { jobId ->
            return hazelcastInstance.jet.getJob(jobId)?.status
         }

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
      return hazelcastInstance.jet.getJob(Util.idFromString(submittedPipeline.jobId))
         ?: error("Pipeline ${submittedPipeline.pipelineSpecId} exists, but it's associated job ${submittedPipeline.jobId} has gone away")
   }

}

