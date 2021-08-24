package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.Util
import com.hazelcast.map.IMap
import com.hazelcast.query.Predicates
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.api.JobStatus
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.badRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

private typealias PipelineSpecId = String
private typealias JetJobId = String

@Component
class PipelineManager(
   private val pipelineFactory: PipelineFactory,
   private val jetInstance: JetInstance,
) {

   private val logger = KotlinLogging.logger {}
   private val submittedPipelines: IMap<JetJobId, SubmittedPipeline> =
      jetInstance.hazelcastInstance.getMap("submittedPipelines")

   fun startPipeline(pipelineSpec: PipelineSpec<*, *>): Pair<SubmittedPipeline, Job> {
      val pipeline = pipelineFactory.createJetPipeline(pipelineSpec)
      logger.info { "Starting pipeline ${pipelineSpec.name}" }
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

      return submittedPipeline to job
   }

   private fun storeSubmittedPipeline(jobId: String, submittedPipeline: SubmittedPipeline) {
      submittedPipelines.put(jobId, submittedPipeline)
   }

   fun getPipelines(): List<RunningPipelineSummary> {
      return jetInstance.jobs
         .map { job ->
            val submittedPipeline = submittedPipelines[job.idString] ?: error("No SubmittedPipeline exists with id ${job.idString}")
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

