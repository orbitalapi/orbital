package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.core.HazelcastInstance


interface ClusterLeaderSelector {
    fun amILeader(): Boolean
}
class HazelcastLeaderSelector(private val hazelcastInstance: HazelcastInstance): ClusterLeaderSelector {
    override fun amILeader(): Boolean {
        // Hazelcast returns the oldest member first.
        val oldestMember = hazelcastInstance.cluster.members.iterator().next()
        return oldestMember.localMember()
    }
}