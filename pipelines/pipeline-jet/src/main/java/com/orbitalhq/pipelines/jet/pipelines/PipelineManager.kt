package com.orbitalhq.pipelines.jet.pipelines

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.Util
import com.hazelcast.jet.core.JobNotFoundException
import com.hazelcast.map.IMap
import com.hazelcast.query.Predicates
import com.orbitalhq.pipelines.jet.api.*
import com.orbitalhq.pipelines.jet.api.transport.PipelineKind
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.ScheduledPipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.log.LogLevel
import com.orbitalhq.pipelines.jet.api.transport.log.LoggingOutputSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.badRequest
import com.orbitalhq.pipelines.jet.source.next
import com.orbitalhq.pipelines.jet.streams.ManagedStream
import lang.taxi.query.TaxiQlQuery
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
      cancelPipelineIfActive(pipelineSpec)
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

   private fun cancelPipelineIfActive(pipelineSpec: PipelineSpec<*, *>) {
      if (hasPipeline(pipelineSpec.id)) {
         terminatePipeline(pipelineSpec.id, deletePipelineRecord = true)
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

   fun getManagedStreams(includeCancelled: Boolean = false): List<RunningPipelineSummary> {
      return getPipelines(kind = PipelineKind.Stream)
         .filter { pipelineSummary ->
            if (!includeCancelled) {
               pipelineSummary.pipeline?.cancelled == false
            } else {
               true
            }
         }
   }
   fun getPipelines(kind: PipelineKind = PipelineKind.Pipeline): List<RunningPipelineSummary> {
      val runningPipelines = submittedPipelines.entries
         .filter { it.value.spec.kind == kind }
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

   fun terminatePipeline(pipelineId: String, deletePipelineRecord: Boolean = false): PipelineStatus {
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
         logger.info { "Terminating running pipeline $pipelineId" }
         val submittedPipeline = getSubmittedPipeline(pipelineId)
         val job = getPipelineJob(submittedPipeline)
         job.cancel()
         if (deletePipelineRecord) {
            submittedPipelines.remove(job.idString)
         } else {
            val cancelledJob = submittedPipeline.copy(cancelled = true)
            storeSubmittedPipeline(job.idString, cancelledJob)
         }
         return pipelineStatus(job, submittedPipeline)
      }
   }

   private fun hasPipeline(pipelineId: String): Boolean {
      val matchingPipelines = submittedPipelines.values(Predicates.sql("pipelineSpecId = '$pipelineId'"))
      return matchingPipelines.isNotEmpty()
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

   fun startPipeline(
      managedStream: ManagedStream,
      sinkSpec: PipelineTransportSpec = LoggingOutputSpec(
         LogLevel.INFO,
         managedStream.name.longDisplayName
      )
   ): Pair<SubmittedPipeline, Job?> {
      val spec = PipelineSpec(
         managedStream.name.longDisplayName,
         StreamingQueryInputSpec(managedStream.query.source),
         null,
         listOf(sinkSpec),
         kind = PipelineKind.Stream
      )
      return startPipeline(spec)
   }

}

