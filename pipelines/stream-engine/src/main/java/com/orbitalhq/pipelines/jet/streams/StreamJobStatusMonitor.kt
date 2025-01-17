package com.orbitalhq.pipelines.jet.streams

import com.google.common.annotations.VisibleForTesting
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.core.JobStatus
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StreamJobStatusMonitor(
   private val hazelcastInstance: HazelcastInstance,
   @VisibleForTesting
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_BEAN_NAME)
   val streamStateCache: MutableMap<String, StreamStatus>,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

//   @Scheduled(fixedDelay = 2000)
//   fun pollForJobStatus() {
//      hazelcastInstance.jet
//         .jobs
//         .toList()
//         .groupBy { it.name }
//         .forEach { (jobName, jobs) ->
//            if (jobName == null) {
//               logger.warn { "Jet engine reports ${jobs.size} jobs without names. States are not managed on these jobs" }
//               return@forEach
//            }
//            // Use the most recently submitted job
//            val job = jobs.sortedBy { it.submissionTime }
//               .last()
//            val streamStatus = streamStateCache[jobName]
//            if (streamStatus == null) {
//               logger.warn { "Found job $jobName which does not have a current state in the Stream state manager" }
//            } else {
//               val (actualStreamStatus,description) = StreamServerStatusService.streamJobStatusFromJetJob(job)
//               if (actualStreamStatus != streamStatus.state) {
//                  logger.warn { "Job ${job.name} is expected in state ${streamStatus.state} but is actually in state $actualStreamStatus (jet status: ${job.status}) - updating" }
//                  streamStateCache[job.name!!] = StreamStatus(
//                     job.name!!,
//                     actualStreamStatus,
//                     errorMessage = description
//                  )
//               }
//            }
//
//         }
}
