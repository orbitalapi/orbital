package com.orbitalhq.queryService

import com.hazelcast.cardinality.CardinalityEstimator
import com.hazelcast.client.ClientService
import com.hazelcast.cluster.Cluster
import com.hazelcast.cluster.Endpoint
import com.hazelcast.collection.IList
import com.hazelcast.collection.IQueue
import com.hazelcast.collection.ISet
import com.hazelcast.config.Config
import com.hazelcast.core.DistributedObject
import com.hazelcast.core.DistributedObjectListener
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.ICacheManager
import com.hazelcast.core.IExecutorService
import com.hazelcast.core.LifecycleService
import com.hazelcast.cp.CPSubsystem
import com.hazelcast.crdt.pncounter.PNCounter
import com.hazelcast.durableexecutor.DurableExecutorService
import com.hazelcast.flakeidgen.FlakeIdGenerator
import com.hazelcast.jet.JetService
import com.hazelcast.logging.LoggingService
import com.hazelcast.map.IMap
import com.hazelcast.multimap.MultiMap
import com.hazelcast.partition.PartitionService
import com.hazelcast.replicatedmap.ReplicatedMap
import com.hazelcast.ringbuffer.Ringbuffer
import com.hazelcast.scheduledexecutor.IScheduledExecutorService
import com.hazelcast.splitbrainprotection.SplitBrainProtectionService
import com.hazelcast.sql.SqlService
import com.hazelcast.topic.ITopic
import com.hazelcast.transaction.HazelcastXAResource
import com.hazelcast.transaction.TransactionContext
import com.hazelcast.transaction.TransactionOptions
import com.hazelcast.transaction.TransactionalTask
import com.nhaarman.mockito_kotlin.mock
import java.util.*
import java.util.concurrent.ConcurrentMap

/**
 * Unlike a TestHazlecastInstance, doesn't do anything.
 * Unlike a mock <> on Hazelcast, this is pre-wired to return empty IMaps etc.
 *
 * Use this in tests that need a HazelcastInstance, but don't need to interact with it.
 * (eg., to satisfy spring wiring, where a mock won't work).
 */
class MockHazelcastInstance : HazelcastInstance {
   override fun getName(): String {
      return "mock"
   }

   override fun <E : Any?> getQueue(name: String): IQueue<E> {
      return mock { }
   }

   override fun <E : Any?> getTopic(name: String): ITopic<E> {
      return mock { }
   }

   override fun <E : Any?> getSet(name: String): ISet<E> {
      return mock { }
   }

   override fun <E : Any?> getList(name: String): IList<E> {
      return mock { }
   }

   override fun <K : Any?, V : Any?> getMap(name: String): IMap<K, V> {
      return mock { }
   }

   override fun <K : Any?, V : Any?> getReplicatedMap(name: String): ReplicatedMap<K, V> {
      return mock { }
   }

   override fun <K : Any?, V : Any?> getMultiMap(name: String): MultiMap<K, V> {
      return mock { }
   }

   override fun <E : Any?> getRingbuffer(name: String): Ringbuffer<E> {
      return mock { }
   }

   override fun <E : Any?> getReliableTopic(name: String): ITopic<E> {
      return mock { }
   }

   override fun getCluster(): Cluster {
      return mock { }
   }

   override fun getLocalEndpoint(): Endpoint {
      return mock { }
   }

   override fun getExecutorService(name: String): IExecutorService {
      return mock { }
   }

   override fun getDurableExecutorService(name: String): DurableExecutorService {
      return mock { }
   }

   override fun <T : Any?> executeTransaction(task: TransactionalTask<T>): T {
      error("Not implemented")
   }

   override fun <T : Any?> executeTransaction(options: TransactionOptions, task: TransactionalTask<T>): T {
      error("Not implemented")
   }

   override fun newTransactionContext(): TransactionContext {
      return mock { }
   }

   override fun newTransactionContext(options: TransactionOptions): TransactionContext {
      return mock { }
   }

   override fun getFlakeIdGenerator(name: String): FlakeIdGenerator {
      return mock { }
   }

   override fun getDistributedObjects(): MutableCollection<DistributedObject> {
      return mock { }
   }

   override fun addDistributedObjectListener(distributedObjectListener: DistributedObjectListener): UUID {
      TODO("Not yet implemented")
   }

   override fun removeDistributedObjectListener(registrationId: UUID): Boolean {
      TODO("Not yet implemented")
   }

   override fun getConfig(): Config {
      TODO("Not yet implemented")
   }

   override fun getPartitionService(): PartitionService {
      TODO("Not yet implemented")
   }

   override fun getSplitBrainProtectionService(): SplitBrainProtectionService {
      return mock { }
   }

   override fun getClientService(): ClientService {
      return mock { }
   }

   override fun getLoggingService(): LoggingService {
      return mock { }
   }

   override fun getLifecycleService(): LifecycleService {
      return mock { }
   }

   override fun <T : DistributedObject?> getDistributedObject(serviceName: String, name: String): T & Any {
      error("Not implemented")
   }

   override fun getUserContext(): ConcurrentMap<String, Any> {
      return mock { }
   }

   override fun getXAResource(): HazelcastXAResource {
      return mock { }
   }

   override fun getCacheManager(): ICacheManager {
      return mock { }
   }

   override fun getCardinalityEstimator(name: String): CardinalityEstimator {
      return mock { }
   }

   override fun getPNCounter(name: String): PNCounter {
      return mock { }
   }

   override fun getScheduledExecutorService(name: String): IScheduledExecutorService {
      return mock { }
   }

   override fun getCPSubsystem(): CPSubsystem {
      return mock { }
   }

   override fun getSql(): SqlService {
      return mock { }
   }

   override fun getJet(): JetService {
      return mock { }
   }

   override fun shutdown() {
   }
}
