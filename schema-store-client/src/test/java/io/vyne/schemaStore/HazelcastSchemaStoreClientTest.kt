package io.vyne.schemaStore

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.impl.HazelcastInstanceFactory
import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemas.DistributedSchemaConfig
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.UUID


class HazelcastSchemaStoreClientTest {
   private lateinit var instance1: HazelcastInstance
   private lateinit var instance2: HazelcastInstance

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
      cfg.clusterName = group // TO prevent conflicts on the same build machine
      cfg.networkConfig.join.multicastConfig.isEnabled = false
      cfg.networkConfig.join.tcpIpConfig.isEnabled = true
      cfg.networkConfig.join.tcpIpConfig.members = listOf("127.0.0.1")
      cfg.addMapConfig(DistributedSchemaConfig.vyneSchemaMapConfig())
      log().info("Starting hazelcast instances, config: $cfg")
      return Hazelcast.newHazelcastInstance(cfg)
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
      waitForSchema(client1, "Client1", listOf("order.taxi:1.0.0"))
      client1.schemaSet().size().should.be.equal(1)
   }

   @Test
   fun `when member schema is updated the cluster schema should update`() {
      // prepare
      val eventPublisher1: ApplicationEventPublisher = mock()
      val eventPublisher2: ApplicationEventPublisher = mock()
      val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher1)
      val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher2)

      val publishedSchemaChangedEvents = mutableListOf<SchemaSetChangedEvent>()

      // subscribe to schema change events on first scheme store.
      Flux.from(client1.schemaChanged).subscribe { schemaChangedEvent ->
         publishedSchemaChangedEvents.add(schemaChangedEvent)
      }
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
      // We expect two schema change events from the first schema store.
      publishedSchemaChangedEvents.size.should.equal(2)
   }

   @Test
   fun `when a member unpublishes a taxi file it is removed from the schema`() {
      // prepare
      val eventPublisher1: ApplicationEventPublisher = mock()
      val eventPublisher2: ApplicationEventPublisher = mock()
      val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher1)
      val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher2)

      val source1 = VersionedSource("order.taxi", "1.0.0", "type Order{}")
      client1.submitSchema(source1)
      waitForSchema(client2, "Client2", listOf("order.taxi:1.0.0"))
      // client1 removed order.taxi
      client1.submitSchemas(emptyList(), listOf(source1.id))
      waitForSchema(client2, "Client2", emptyList())
   }

   @Test
   fun `A member starts up and publishes a schema referencing a type published by another member`() {
      // prepare
      val eventPublisher1: ApplicationEventPublisher = mock()
      val eventPublisher2: ApplicationEventPublisher = mock()
      val client1 = HazelcastSchemaStoreClient(instance1, eventPublisher = eventPublisher1)
      val client2 = HazelcastSchemaStoreClient(instance2, eventPublisher = eventPublisher2)

      // this will yield compilation error as OrderId is not defined yet.
      val source1 = VersionedSource("order.taxi", "1.0.0", "model Order{orderId: OrderId}")
      client1.submitSchema(source1)
      waitForSchema(client2, "Client2", listOf("order.taxi:1.0.0"))
      // Schema should have a source with compilation error
      client1.schemaSet().sources.first().errors.size.should.equal(1)
      // client2 publishes OrderId definition
      val source2 = VersionedSource("orderId.taxi", "1.0.0", "type OrderId inherits String")
      client1.submitSchema(source2)
      // assert
      waitForSchema(client2, "Client2", listOf("orderId.taxi:1.0.0", "order.taxi:1.0.0"))
      // schema should compile without any errors
      client1.schemaSet().sources.all { it.errors.isEmpty() }.should.`true`
   }

   private fun waitForSchema(client: HazelcastSchemaStoreClient, clientName: String, expectedSchemas: List<String>) {
      await()
         .atMost(Duration.FIVE_MINUTES)
         .pollInterval(Duration.ONE_HUNDRED_MILLISECONDS)
         .conditionEvaluationListener { evaluatedCondition ->
            log().info(
               "Condition evaluated as ${evaluatedCondition.isSatisfied} $clientName expecting $expectedSchemas and got ${client.schemaSet().allSources.map { it.id }}"
            )
         }
         .until<Boolean> {
            val schema = client.schemaSet()
            schema.size() == expectedSchemas.size &&
               schema.allSources.map { it.id }.sorted().containsAll(expectedSchemas.sorted())
         }
   }
}
