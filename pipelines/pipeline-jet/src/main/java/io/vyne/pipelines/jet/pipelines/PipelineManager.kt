package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.Util
import com.hazelcast.map.IMap
import com.hazelcast.query.Predicates
import io.vyne.pipelines.jet.api.JobStatus
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
      val pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>
   ) :
      Serializable

   private val logger = KotlinLogging.logger {}
   private val submittedPipelines: IMap<JetJobId, SubmittedPipeline> =
      jetInstance.hazelcastInstance.getMap("submittedPipelines")

   private val scheduledPipelines: IMap<String, ScheduledPipeline> =
      jetInstance.hazelcastInstance.getMap("scheduledPipelines")

   fun startPipeline(pipelineSpec: PipelineSpec<*, *>): Pair<SubmittedPipeline, Job?> {
      val pipeline = pipelineFactory.createJetPipeline(pipelineSpec)
      logger.info { "Starting pipeline ${pipelineSpec.name}" }
      return if (pipelineSpec.input is ScheduledPipelineTransportSpec) {
         scheduleJobToBeExecuted(pipelineSpec as PipelineSpec<ScheduledPipelineTransportSpec, *>)
         SubmittedPipeline(
            pipelineSpec.name,
            null,
            pipelineSpec,
            pipeline.toDotString(),
            DotVizUtils.dotVizToGraphNodes(pipeline.toDotString()),
            cancelled = false
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

   private fun scheduleJobToBeExecuted(pipelineSpec: PipelineSpec<ScheduledPipelineTransportSpec, *>) {
      val schedule = CronSequenceGenerator(pipelineSpec.input.pollSchedule)
      val nextScheduledRunTime = schedule.next(Instant.now())
      logger.info("The pipeline \"${pipelineSpec.name}\" is next scheduled to run at ${nextScheduledRunTime}.")
      scheduledPipelines.put(
         pipelineSpec.id,
         ScheduledPipeline(nextScheduledRunTime, pipelineSpec)
      )
   }

   @Scheduled(fixedRate = 1000)
   fun runScheduledPipelinesIfAny() {
      scheduledPipelines.entries.forEach {
         if (scheduledPipelines.isLocked(it.key)) {
            return@forEach
         }
         scheduledPipelines.lock(it.key, 5, TimeUnit.SECONDS)
         if (it.value.nextRunTime.isAfter(Instant.now())) {
            logger.trace("Skipping pipeline \"${it.value.pipelineSpec.name}\" as it is next scheduled to run at ${it.value.nextRunTime}.")
            return@forEach
         }
         logger.info("A scheduled run of the pipeline \"${it.value.pipelineSpec.name}\" starting.")
         val pipeline = pipelineFactory.createJetPipeline(it.value.pipelineSpec)
         val jobId = jetInstance.newJob(pipeline)
         storeSubmittedPipeline(
            jobId.idString, SubmittedPipeline(
               it.value.pipelineSpec.name,
               jobId.idString,
               it.value.pipelineSpec,
               pipeline.toDotString(),
               DotVizUtils.dotVizToGraphNodes(pipeline.toDotString()),
               cancelled = false
            )
         )
         scheduleJobToBeExecuted(it.value.pipelineSpec)
         scheduledPipelines.unlock(it.key)
      }
   }

   private fun storeSubmittedPipeline(jobId: String, submittedPipeline: SubmittedPipeline) {
      submittedPipelines.put(jobId, submittedPipeline)
   }

   fun getPipelines(): List<RunningPipelineSummary> {
      return jetInstance.jobs
         .map { job ->
            val submittedPipeline =
               submittedPipelines[job.idString] ?: error("No SubmittedPipeline exists with id ${job.idString}")
            val status = pipelineStatus(job, submittedPipeline)
            RunningPipelineSummary(
               submittedPipeline,
               status
            )
         }
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
      val submittedPipeline = getSubmittedPipeline(pipelineId)
      val job = getPipelineJob(submittedPipeline)
      job.cancel()
      val cancelledJob = submittedPipeline.copy(cancelled = true)
      storeSubmittedPipeline(job.idString, cancelledJob)
      return pipelineStatus(job, submittedPipeline)
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
      val submittedPipeline = getSubmittedPipeline(pipelineSpecId)
      val job = getPipelineJob(submittedPipeline)
      val status = pipelineStatus(job, submittedPipeline)
      return RunningPipelineSummary(
         submittedPipeline,
         status
      )
   }

   private fun getPipelineJob(
      submittedPipeline: SubmittedPipeline,
   ): Job {
      return jetInstance.getJob(Util.idFromString(submittedPipeline.jobId))
         ?: error("Pipeline ${submittedPipeline.pipelineSpecId} exists, but it's associated job ${submittedPipeline.jobId} has gone away")
   }

}

