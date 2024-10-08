package com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class DistributedSchemaStoreClientTest{
    private lateinit var schemaStore1: DistributedSchemaStoreClient
    private lateinit var schemaStore2: DistributedSchemaStoreClient
    private lateinit var hz1: HazelcastInstance
    private lateinit var hz2: HazelcastInstance
    private val hzFactory = TestHazelcastInstanceFactory(2)
    private val ordersPackageV1 = SourcePackage(
        PackageMetadata.from("com.foo", "Orders", "0.1.0"),
        sources = listOf(
            VersionedSource(
                name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent()
            )
        ),
        additionalSources = emptyMap()
    )

    private val tradesPackageV1 = SourcePackage(
        PackageMetadata.from("com.foo", "Trades", "0.1.0"),
        sources = listOf(
            VersionedSource(
                name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Trade {
                 tradeId: String
              }
         }
      """.trimIndent()
            )
        ),
        additionalSources = emptyMap()
    )
    @BeforeEach
    fun setup() {
        hz1 = hzFactory.newHazelcastInstance()
        hz2 = hzFactory.newHazelcastInstance()
        schemaStore1 = DistributedSchemaStoreClient(hz1)
        schemaStore2 = DistributedSchemaStoreClient(hz2)
    }

    @AfterEach
    fun terminate() {
        hzFactory.shutdownAll()
    }

    @Test
    fun `A schema change occurred on node1 should be replicated to node2`() {
        StepVerifier
            .create(schemaStore2.schemaChanged)
            .expectSubscription()
            .then { schemaStore1.submitPackage(ordersPackageV1) }
            .then { schemaStore1.submitPackage(tradesPackageV1) }
            .expectNextMatches { event ->
                event.newSchemaSet.taxiSchemas.first().hasType("foo.bar.Order")
            }.thenCancel()
            .verify()
    }
}