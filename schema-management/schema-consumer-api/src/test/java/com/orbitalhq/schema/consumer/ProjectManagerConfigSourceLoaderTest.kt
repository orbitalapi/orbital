package com.orbitalhq.schema.consumer

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.ParsedSource
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asParsedPackages
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schema.publisher.PublisherType
import com.orbitalhq.schema.publisher.loaders.AddChangesToChangesetResponse
import com.orbitalhq.schema.publisher.loaders.AvailableChangesetsResponse
import com.orbitalhq.schema.publisher.loaders.CreateChangesetResponse
import com.orbitalhq.schema.publisher.loaders.FinalizeChangesetResponse
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schema.publisher.loaders.ProjectTransportConfig
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schema.publisher.loaders.SetActiveChangesetResponse
import com.orbitalhq.schema.publisher.loaders.UpdateChangesetResponse
import com.orbitalhq.schemas.SchemaSetChangedEvent
import org.junit.jupiter.api.Test
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import reactor.test.StepVerifierOptions
import java.net.URI
import java.util.concurrent.SubmissionPublisher
import kotlin.math.sin

class ProjectManagerConfigSourceLoaderTest {
    @Test
    fun `an error in loading sources does not stop observing schema change events`() {
        val schemaEventSource = DummySchemaChangedEventProvider()
        val testObject = ProjectManagerConfigSourceLoader(schemaEventSource, DummyProjectLoaderManager(), "connections.conf")
        StepVerifier
            .create(testObject.contentUpdated)
            .expectSubscription()
            .expectNextMatches {
                true
            }
            .then {
                // Upon 'expectSubscription'  we will call loadNow() on 2 SchemaPackageTransport. The first of the SchemaPackageTransport
                // will emit Mono.error
                //We want to ensure that an error on an underlying SchemaPackageTransport won't kill subscription to schema changed events.
                schemaEventSource.sink.emitNext(schemaChangedEvent(), Sinks.EmitFailureHandler.FAIL_FAST)
            }.expectNextMatches { next ->
                true
            }
            .thenCancel()
            .verify()


    }

    fun schemaChangedEvent(): SchemaSetChangedEvent {
        val oldSource = VersionedSource("order.taxi", "1.0.0", "type Order {}")
        val newSource = VersionedSource("client.taxi", "1.1.1", "type Client {}")
        val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(oldSource).asParsedPackage()), 1)
        val newSchemaSet =
            SchemaSet.fromParsed(listOf(ParsedSource(oldSource), ParsedSource(newSource)).asParsedPackages(), 2)
        return SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)
    }
}

class DummySchemaChangedEventProvider : SchemaChangedEventProvider {
   val sink = Sinks.many().multicast().onBackpressureBuffer<SchemaSetChangedEvent>()
    override val schemaChanged: Publisher<SchemaSetChangedEvent>
        get() = sink.asFlux()


}

class DummySchemaPackageTransport(private val sourcePackage: Mono<SourcePackage>): SchemaPackageTransport {
    override fun start(): Flux<SourcePackage> {
        TODO("Not yet implemented")
    }

   override fun stop() {
      TODO("Not yet implemented")
   }

    override fun loadNow(): Mono<SourcePackage> {
       return sourcePackage
    }

    override val loaderStatus: Flux<LoaderStatus>
        get() = TODO("Not yet implemented")
    override val description: String
        get() = TODO("Not yet implemented")
    override val root: URI
        get() = TODO("Not yet implemented")

    override fun listUris(): Flux<URI> {
        TODO("Not yet implemented")
    }

    override fun readUri(uri: URI): Mono<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun isEditable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createChangeset(name: String): Mono<CreateChangesetResponse> {
        TODO("Not yet implemented")
    }

    override fun addChangesToChangeset(
        name: String,
        edits: List<VersionedSource>
    ): Mono<AddChangesToChangesetResponse> {
        TODO("Not yet implemented")
    }

    override fun finalizeChangeset(name: String): Mono<FinalizeChangesetResponse> {
        TODO("Not yet implemented")
    }

    override fun updateChangeset(name: String, newName: String): Mono<UpdateChangesetResponse> {
        TODO("Not yet implemented")
    }

    override fun getAvailableChangesets(): Mono<AvailableChangesetsResponse> {
        TODO("Not yet implemented")
    }

    override fun setActiveChangeset(branchName: String): Mono<SetActiveChangesetResponse> {
        TODO("Not yet implemented")
    }

    override val packageIdentifier: PackageIdentifier
        get() = TODO("Not yet implemented")
    override val publisherType: PublisherType
        get() = TODO("Not yet implemented")
    override val config: ProjectTransportConfig
        get() = TODO("Not yet implemented")

}
class DummyProjectLoaderManager: ProjectLoaderManager {
    private val sourcePackage: SourcePackage
    init {
        val oldSource = VersionedSource("order.taxi", "1.0.0", "type Order {}")
        val newSource = VersionedSource("client.taxi", "1.1.1", "type Client {}")
        val oldSchemaSet = SchemaSet.fromParsed(listOf(ParsedSource(oldSource).asParsedPackage()), 1)
        val newSchemaSet =
            SchemaSet.fromParsed(listOf(ParsedSource(oldSource), ParsedSource(newSource)).asParsedPackages(), 2)
        sourcePackage = newSchemaSet.packages.first()
    }
    override fun getLoaderOrNull(packageIdentifier: PackageIdentifier): SchemaPackageTransport? {
        TODO("Not yet implemented")
    }

    override fun getLoader(packageIdentifier: PackageIdentifier): SchemaPackageTransport {
        TODO("Not yet implemented")
    }

    override val loaders: List<SchemaPackageTransport>
        get() = listOf(DummySchemaPackageTransport(Mono.error(IllegalArgumentException("faulty transport"))),
            DummySchemaPackageTransport(Mono.just(sourcePackage))
        )
    override val editableLoaders: List<SchemaPackageTransport>
        get() = TODO("Not yet implemented")

}

fun ParsedSource.asParsedPackage(
    organisation: String = "com.foo",
    name: String = "test",
    version: String = "1.0.0"
): ParsedPackage {
    return ParsedPackage(
        PackageMetadata.from(organisation, name, version),
        listOf(this),
        emptyMap()

    )
}
