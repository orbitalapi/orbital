package com.orbitalhq.pipelines.jet.streams

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.core.JobStatus
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamName
import com.orbitalhq.pipelines.jet.api.streams.StreamStateWithJobStates
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.websocket.OrbitalWebSocketConfiguration
import com.orbitalhq.spring.http.websocket.WebSocketController
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration


/**
 * Service which provides status of running streams,
 * as well as metrics about the stream server cluster (eg., number of nodes).
 *
 */
@Controller
class StreamServerStatusService(
   private val hazelcastInstance: HazelcastInstance,
   streamStateManager: StreamStateManager,
   private val stateUpdatesPublisher: StreamStateUpdatesPublisher,
   private val objectMapper: ObjectMapper,
   private val orbitalWebSocketConfiguration: OrbitalWebSocketConfiguration
) : WebSocketController {

   private val replayingStatusUpdateSink = Sinks.many().replay().latest<Map<StreamName,StreamStateWithJobStates>>()
   private val replayingStatusUpdates = replayingStatusUpdateSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}

      fun streamJobStatusFromJetJob(job: Job): StreamJobStateEvent {
         val suspensionCause = if (job.status == JobStatus.SUSPENDED) {
            job.readVolatile({ it.suspensionCause }, JobStatus.SUSPENDED, null).block()
         } else null
         val isUserCancelled = if (job.status == JobStatus.FAILED) {
            job.readVolatile({ it.isUserCancelled }, JobStatus.FAILED, false).block() ?: false
         } else {
            false
         }
         val description = suspensionCause?.description()
            ?: if (isUserCancelled) "UserCancelled" else null

         return StreamJobStateEvent(
            job.id.toString(),
            job.name!!,
            status = StreamJobStateEvent.JobStatus.valueOf(job.status.name),
            description = description
         )
      }
   }

   init {
      stateUpdatesPublisher.stateUpdates
         .subscribe { event -> replayingStatusUpdateSink.tryEmitNext(event) }
//      stateUpdatesPublisher.stateUpdates
//         .subscribe { streamStatuses ->
//            streamServerStatusSink.tryEmitNext(
//               StreamServerStatusEvent(
//                  hazelcastInstance.cluster.members.size,
//                  streamStatuses
//               )
//            )
//         }

   }

   @MessageMapping("stream-server-status")
   fun getStreamServerStatusEvents(): Flux<Map<StreamName,StreamStateWithJobStates>> {
      return replayingStatusUpdateSink.asFlux()
   }


   override val paths: List<String> = listOf("/api/streams/status")

   @PreAuthorize("hasAuthority('${VynePrivileges.EditPipelines}')")
   override fun handle(session: WebSocketSession): Mono<Void> {
      val outbound = replayingStatusUpdates
         .map { event ->
            session.textMessage(
               objectMapper.writeValueAsString(event)
            )
         }

      return orbitalWebSocketConfiguration.applyPingConfiguration(this, session, outbound)
   }
}

/**
 * We see race conditions when a job enters a state like Suspended, but then asking
 * for a suspension reason throws an exception.
 * So, we wrap the exception, and read for a short period.
 */
private fun <T> Job.readVolatile(
   accessor: (Job) -> T?,
   whileInState: JobStatus,
   default: T,
   timeout: Duration = Duration.ofMillis(100),
   pollPeriod: Duration = Duration.ofMillis(10)
): Mono<T> {
   return Flux.interval(pollPeriod)
      .map {
         if (this.status != whileInState) {
            return@map default
         }
         try {
            accessor(this)
         } catch (e: IllegalStateException) {
            null
         }
      }
      .filter { it != null }
      .next()
      .timeout(timeout, Mono.justOrEmpty(default)) as Mono<T>
}

