package io.vyne.schemaStore


import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.HazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.isA
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.fail


class HazelcastSchemaStoreClientTest {
   lateinit var instance1: HazelcastInstance
   lateinit var instance2: HazelcastInstance

   @Before
   fun before() {
      val group = "hazel-test-cluster-${UUID.randomUUID()}"
      instance1 = newHazelcastInstance("hazelcastInstance-cluster-node1", group)
      instance2 = newHazelcastInstance("hazelcastInstance-cluster-node2", group)
   }

   @After
   fun after() {
      HazelcastInstanceFactory.terminateAll()
   }

   private fun newHazelcastInstance(instanceName: String, group: String): HazelcastInstance {
      val cfg = Config()
      cfg.instanceName = instanceName
      cfg.groupConfig.name = group// TO prevent conflicts on the same build machine
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
      for (i in 1..200) {
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
      val eventPublisher1: ApplicationEventPublisher = mock()
      val eventPublisher2: ApplicationEventPublisher = mock()
      val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher1)
      val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher2)

      val source1 = VersionedSource("order.taxi", "1.0.0", "type Order{}")
      client1.submitSchema(source1)
      waitForSchema(client2, "Client2", listOf("order.taxi:1.0.0"))

      // act
      val source2 = VersionedSource("order.taxi", "2.0.0", "type Order{id: String}")
      client1.submitSchema(source2)

      // assert
      waitForSchema(client2, "Client2", listOf("order.taxi:2.0.0"))
      val schemaSet = client1.schemaSet()
      schemaSet.size().should.be.equal(1)
      schemaSet.sources[0].source.content.should.be.equal("type Order{id: String}")

      // assert local schemaSet changed events are pushed
      verify(eventPublisher1).publishEvent(SchemaSetChangedEvent(
         null, SchemaSet.fromParsed(listOf(), 1))
      )
      verify(eventPublisher1).publishEvent(SchemaSetChangedEvent(
         SchemaSet.fromParsed(listOf(), 1),
         SchemaSet.fromParsed(listOf(ParsedSource(source1)), 2))
      )
      verify(eventPublisher1).publishEvent(SchemaSetChangedEvent(
         SchemaSet.fromParsed(listOf(ParsedSource(source1)), 2),
         SchemaSet.fromParsed(listOf(ParsedSource(source2)), 3))
      )
   }

}
