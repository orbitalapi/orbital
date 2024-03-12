package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.cluster.MembershipEvent
import com.hazelcast.cluster.MembershipListener
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.pipelines.jet.api.streams.StreamServerStatusEvent
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
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
   private val stateUpdatesPublisher: StreamStateUpdatesPublisher
) {

   private val streamServerStatusSink = Sinks.many().replay().latest<StreamServerStatusEvent>()

   private val clusterSizeChanged = Sinks.many().replay().latest<Int>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      val streamStateUpdates = stateUpdatesPublisher.stateUpdates
         .startWith(streamStateManager.getAllStreamStates())
      Flux.combineLatest(clusterSizeChanged.asFlux(), streamStateUpdates) { clusterSize, streamStatuses ->
         StreamServerStatusEvent(clusterSize, streamStatuses)
      }.subscribe { streamServerStatusSink.tryEmitNext(it) }

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
}

