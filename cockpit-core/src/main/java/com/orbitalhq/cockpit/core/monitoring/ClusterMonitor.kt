package com.orbitalhq.cockpit.core.monitoring

import com.hazelcast.cluster.Cluster
import com.hazelcast.cluster.MembershipEvent
import com.hazelcast.cluster.MembershipListener
import com.hazelcast.core.HazelcastInstance
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Simply watches the hazelcast cluster and logs.
 * Useful for monitoring toolig / prod etc.
 */
@Component
class ClusterMonitor(private val hazelcastInstance: HazelcastInstance) : MembershipListener {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      val cluster = hazelcastInstance.cluster
      cluster.addMembershipListener(this)
      logger.info { "Cluster initialized. ${clusterDescription(cluster)}" }
   }

   private fun clusterDescription(cluster: Cluster):String {
      return "Cluster currently contains ${cluster.members.size} members:  ${cluster.members.joinToString {
         val thisMemberSuffix = if (it.localMember()) { " (this)" } else ""
         "${it.address}$thisMemberSuffix"
      }}"
   }

   override fun memberAdded(membershipEvent: MembershipEvent) {
      logger.info { "Cluster members changed - new member added (${membershipEvent.member.address}). ${clusterDescription(membershipEvent.cluster)}" }
   }

   override fun memberRemoved(membershipEvent: MembershipEvent) {
      logger.info { "Cluster members changed - member removed (${membershipEvent.member.address}). ${clusterDescription(membershipEvent.cluster)}" }
   }
}
