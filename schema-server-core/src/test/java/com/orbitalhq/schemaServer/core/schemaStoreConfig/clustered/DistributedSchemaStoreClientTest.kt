package com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered

import arrow.core.Either
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.publisher.ExpiringSourcesStore
import com.orbitalhq.schema.publisher.KeepAlivePackageSubmission
import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered.DistributedSchemaStoreClient.Companion.SCHEMA_SOURCES_MAP
import com.orbitalhq.schemas.Schema
import lang.taxi.CompilationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class DistributedSchemaStoreClientTest{
    private lateinit var schemaStore1: DistributedSchemaStoreClient
    private lateinit var schemaStore2: DistributedSchemaStoreClient
    private lateinit var hz1: HazelcastInstance
    private lateinit var hz2: HazelcastInstance
    private lateinit var expiringSources: ExpiringSourcesStore
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
        setUpNode1()
        setUpNode2()
    }

    private fun setUpNode1() {
        val uuid1 =  hz1.cluster.localMember.uuid
        val packagesByIdMap = hz1.getMap<String, ParsedPackage>(SCHEMA_SOURCES_MAP)
        expiringSources = ExpiringSourcesStore(emptyList(), hz1.getMap("expiringSourceMap"))
        schemaStore1 = DistributedSchemaStoreClient(hz1, packagesByIdMap)
        ParsedPackageMapEventListenerConfigurer(expiringSources, schemaStore1, packagesByIdMap, uuid1)
    }

    private fun setUpNode2() {
        val uuid2 = hz2.cluster.localMember.uuid
        val packagesByIdMap = hz2.getMap<String, ParsedPackage>(SCHEMA_SOURCES_MAP)
        val expiringSources2 = ExpiringSourcesStore(emptyList(), hz2.getMap("expiringSourceMap"))
        schemaStore2 = DistributedSchemaStoreClient(hz2, packagesByIdMap)
        ParsedPackageMapEventListenerConfigurer(expiringSources2, schemaStore2, packagesByIdMap, uuid2)
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
            .then {
                schemaStore1.submitUpdates(expiringSources.submitSources(KeepAlivePackageSubmission(ordersPackageV1)))
            }
            .then {
                schemaStore1.submitUpdates(expiringSources.submitSources(KeepAlivePackageSubmission(tradesPackageV1)))
            }
            .expectNextMatches { event ->
                event.newSchemaSet.taxiSchemas.first().hasType("foo.bar.Order")
            }.thenCancel()
            .verify()
    }
}

class TestSchemaPublisher: SchemaPublisherTransport {
    override fun submitMonitoredPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
        TODO("Not yet implemented")
    }

    override fun submitPackage(submission: SourcePackage): Either<CompilationException, Schema> {
        TODO("Not yet implemented")
    }

    override fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema> {
        TODO("Not yet implemented")
    }

}