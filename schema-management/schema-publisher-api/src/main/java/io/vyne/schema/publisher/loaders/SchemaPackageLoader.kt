package io.vyne.schema.publisher.loaders

import io.vyne.schema.api.PackageMetadata
import io.vyne.schema.api.SchemaPackage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Loads a Schema Package.
 *
 * Implementations will often contain a SchemaPackageTransport and a SchemaPackageAdaptor
 * Since: Schemas3
 */
interface SchemaPackageLoader {

   /**
    * Returns a Flux of schema packages.
    * Should emit a new message whenever sources change.
    */
   fun load(): Flux<SchemaPackage>


}


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
   fun start(): Flux<SchemaPackage>

   fun listUris(): Flux<URI>
   fun readUri(uri: URI): Mono<ByteArray>
}

interface SchemaSourcesAdaptor {
   fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata>
   fun convert(source: PackageMetadata, transport: SchemaPackageTransport): Mono<SchemaPackage>
}

