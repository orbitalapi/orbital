package com.orbitalhq.pipelines.jet.streams

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.cluster.MembershipEvent
import com.hazelcast.cluster.MembershipListener
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.http.ServicesConfig.Companion.STREAM_SERVER_NAME
import com.orbitalhq.pipelines.jet.api.streams.StreamServerStatusEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.streams.StreamStatusUpdateRequest
import com.orbitalhq.pipelines.jet.pipelines.PipelineService
import com.orbitalhq.spring.http.websocket.WebSocketController
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks


/**
 * Service which provides status of running streams,
 * as well as metrics about the stream server cluster (eg., number of nodes).
 *
 */
@Controller
class StreamServerStatusService(
   private val hazelcastInstance: HazelcastInstance,
   private val streamStateManager: StreamStateManager,
   private val stateUpdatesPublisher: StreamStateUpdatesPublisher,
   private val objectMapper: ObjectMapper,
) : WebSocketController {

   private val streamServerStatusSink = Sinks.many().replay().latest<StreamServerStatusEvent>()

   //   private val sink = Sinks.many().replay().latest<StreamServerStatusWithConnectionMessage>()
   private val statusUpdates = streamServerStatusSink.asFlux()
   private val clusterSizeChanged = Sinks.many().replay().latest<Int>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      val streamStateUpdates = stateUpdatesPublisher.stateUpdates
         .startWith(streamStateManager.getAllStreamStates())
// TODO : Why are we doing this subscribe + sink stuff? Shouldn't we just set
      // streamServerStatusSink to this flux?
      Flux.combineLatest(clusterSizeChanged.asFlux(), streamStateUpdates) { clusterSize, streamStatuses ->
         StreamServerStatusEvent(clusterSize, streamStatuses)
      }
         .subscribe { streamServerStatusSink.tryEmitNext(it) }

      hazelcastInstance.cluster.addMembershipListener(object : MembershipListener {
         override fun memberAdded(membershipEvent: MembershipEvent) = emitClusterSize()
         override fun memberRemoved(membershipEvent: MembershipEvent) = emitClusterSize()
      })
      emitClusterSize() // Initial emit
   }

   private fun emitClusterSize() {
      val size = hazelcastInstance.cluster.members.size
      clusterSizeChanged.tryEmitNext(size)
   }

   @MessageMapping("stream-server-status")
   fun clusterSize(): Flux<StreamServerStatusEvent> {
      return streamServerStatusSink.asFlux()
   }


   override val paths: List<String> = listOf("/api/streams/status")

   override fun handle(session: WebSocketSession): Mono<Void> {
      return session.send(
         statusUpdates
            .map { event ->
               session.textMessage(
                  objectMapper.writeValueAsString(event)
               )
            }
      )
   }
}


data class StreamServerStatusWithConnectionMessage(
   val connectionStatus: ConnectionStatus,
   val streamServerState: StreamServerStatusEvent?
)
