package io.vyne.schemaStore


import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.HazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.utils.log
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.concurrent.TimeUnit
import kotlin.test.fail


class HazelcastSchemaStoreClientTest {
   lateinit var instance1: HazelcastInstance
   lateinit var instance2: HazelcastInstance

   @Before
   fun before() {
      instance1 = newHazelcastInstance("hazelcastInstance-cluster-node1")
      instance2 = newHazelcastInstance("hazelcastInstance-cluster-node2")
   }

   @After
   fun after() {
      HazelcastInstanceFactory.terminateAll()
   }

   private fun newHazelcastInstance(instanceName: String): HazelcastInstance {
      val cfg = Config()
      cfg.instanceName = instanceName
      cfg.groupConfig.name = "hazel-test-cluster"
      cfg.networkConfig.join.multicastConfig.isEnabled = false
      cfg.networkConfig.join.tcpIpConfig.isEnabled = true
      cfg.networkConfig.join.tcpIpConfig.members = listOf("127.0.0.1")
      log().info("Starting hazelcast instances, config: ${cfg}")
      return Hazelcast.newHazelcastInstance(cfg)
   }

   private fun waitForSchema(client: HazelcastSchemaStoreClient, clientName: String, expectedSchemas: List<String>) {
      val condition = {
         client.schemaSet().size() == expectedSchemas.size &&
            client.schemaSet().allSources.map { it.id }.sorted().containsAll(expectedSchemas.sorted())
      }
      for (i in 1..50) {
         if (condition()) {
            return
         }
         log().info("${clientName} waiting for schema to have ${expectedSchemas}")
         TimeUnit.MILLISECONDS.sleep(250L)
      }
      fail("${clientName} expecting ${expectedSchemas} but got ${client.schemaSet().allSources.map { it.id }}")
   }

   @Test
    fun `when member disconnects from the cluster all it's schemas should be deleted`() {
       // prepare
       val eventPublisher: ApplicationEventPublisher = mock()
       val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher)
       val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher)

       client1.submitSchema("order.taxi", "1.0.0", "type Order{}")
       waitForSchema(client2, "Client2", listOf("order.taxi:1.0.0"))

       client2.submitSchema("trade.taxi", "1.0.0", "type Trade{}")
       waitForSchema(client1,  "Client1", listOf("order.taxi:1.0.0", "trade.taxi:1.0.0"))

       // act
       instance2.shutdown()

       // assert
       waitForSchema(client1,  "Client1", listOf("order.taxi:1.0.0"))
      client1.schemaSet().size().should.be.equal(1)
    }

   @Test
   fun `when member schema is updated the cluster schema should update`() {
      // prepare
      val eventPublisher: ApplicationEventPublisher = mock()
      val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher)
      val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher)

      client1.submitSchema("order.taxi", "1.0.0", "type Order{}")
      waitForSchema(client2, "Client2", listOf("order.taxi:1.0.0"))

      // act
      client1.submitSchema("order.taxi", "2.0.0", "type Order{id: String}")

      // assert
      waitForSchema(client2, "Client2", listOf("order.taxi:2.0.0"))
      client1.schemaSet().size().should.be.equal(1)
      client1.schemaSet().sources[0].source.content.should.be.equal("type Order{id: String}")
   }

}
