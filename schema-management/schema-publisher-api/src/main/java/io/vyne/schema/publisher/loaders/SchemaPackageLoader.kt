package io.vyne.schema.publisher.loaders

import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI


/**
 * Loads schema metadata (and often the schema itself) from
 * some location.
 *
 * Responsible for emitting a subtype of PackageMetadata, which should
 * include source files for a downstream SchemaSourcesAdaptor to convert into a SchemaPackage.
 *
 * eg:  Transport that encapsulates loading schemas of some form from a git repository.
 */
interface SchemaPackageTransport {
   /**
    * If called multiple times, the same Flux<> should be returned
    */
   fun start(): Flux<SourcePackage>

   fun listUris(): Flux<URI>
   fun readUri(uri: URI): Mono<ByteArray>
}

interface SchemaSourcesAdaptor {
   fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata>
   fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage>
}

