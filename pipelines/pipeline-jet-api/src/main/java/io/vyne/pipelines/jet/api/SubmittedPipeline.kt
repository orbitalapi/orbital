package io.vyne.pipelines.jet.api

import io.vyne.pipelines.jet.api.transport.PipelineSpec
import java.io.Serializable
import java.time.Instant

/**
 * This is a duplicate of the Hazelcast Jet JobStatus enum.
 * Duplicated here to avoid dependency on Hazelcast Jet
 */
enum class JobStatus {
   /**
    * The job is submitted but hasn't started yet. A job also enters this
    * state when its execution was interrupted (e.g., due to a cluster member
    * failing), before it is started again.
    */
   NOT_RUNNING,

   /**
    * The job is in the initialization phase on a new coordinator.
    */
   STARTING,

   /**
    * The job is currently running.
    */
   RUNNING,

   /**
    * The job is suspended and it can be manually resumed.
    */
   SUSPENDED,

   /**
    * The job is suspended and is exporting the snapshot. It cannot be resumed
    * until the export is finished and status is [.SUSPENDED] again.
    */
   SUSPENDED_EXPORTING_SNAPSHOT,

   /**
    * The job is currently being completed.
    */
   COMPLETING,

   /**
    * The job has failed with an exception.
    */
   FAILED,

   /**
    * The job has completed successfully.
    */
   COMPLETED,

   /**
    * The job has been scheduled to be executed at certain time.
    */
   SCHEDULED,

   /**
    * The job was terminated by a user
    */
   CANCELLED;

   /**
    * Returns `true` if this state is terminal - a job in this state
    * will never have any other state and will never execute again. It's
    * [.COMPLETED] or [.FAILED].
    */
   val isTerminal: Boolean
      get() = this == COMPLETED || this == FAILED

}

data class SubmittedPipeline(
   val name: String,
   val jobId: String?, // Job id will be null for pipelines with a scheduled input as a new job is submitted for each execution
   val spec: PipelineSpec<*, *>,
   val dotViz: String,
   val graph: DagDataset,
   val cancelled: Boolean
) : Serializable {
   val pipelineSpecId: String = spec.id
}

data class PipelineStatus(
   val name: String,
   val id: String,
   val status: JobStatus,
   val submissionTime: Instant,
   val metrics: PipelineMetrics
)

data class PipelineMetrics(
   val receivedCount: List<MetricValueSet>,
   val emittedCount: List<MetricValueSet>,
   val inflight: List<MetricValueSet>,
   val queueSize: List<MetricValueSet>
)

data class MetricValue(
   val value: Any,
   val timestamp: Instant
)

data class MetricValueSet(
   val address: String,
   val measurements: List<MetricValue>,
   val latestValue: MetricValue?
)


data class RunningPipelineSummary(
   val pipeline: SubmittedPipeline?,
   val status: PipelineStatus
)

// Classes structure to support graph gen in the UI
data class DagGraphNode(val id: String, val label: String) : Serializable
data class DagGraphLink(val source: String, val target: String, val label: String) : Serializable
data class DagDataset(val nodes: List<DagGraphNode>, val links: List<DagGraphLink>) : Serializable
