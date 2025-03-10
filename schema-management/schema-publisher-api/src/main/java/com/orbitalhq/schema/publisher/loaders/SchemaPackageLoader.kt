package com.orbitalhq.schema.publisher.loaders

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigSourceWriter
import com.orbitalhq.schema.publisher.PublisherType
import lang.taxi.packages.SourcesType
import lang.taxi.packages.TaxiPackageProject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


data class CreateChangesetResponse(
   val changeset: Changeset
)

data class AddChangesToChangesetResponse(
   val changesetOverview: ChangesetOverview?
)

data class FinalizeChangesetResponse(
   val changesetOverview: ChangesetOverview,
   val changeset: Changeset,
   val link: String? = null,
)

data class UpdateChangesetResponse(
   val changeset: Changeset,
)

data class AvailableChangesetsResponse(
   val changesets: List<Changeset>
)

data class ChangesetOverview(
   val additions: Int,
   val changedFiles: Int,
   val deletions: Int,
   val author: String,
   val description: String,
   val lastUpdated: Date
)


data class Changeset(
   val name: String,
   val isActive: Boolean,
   val isDefault: Boolean,
   val packageIdentifier: PackageIdentifier
)

data class SetActiveChangesetResponse(
   val changeset: Changeset,
   val changesetOverview: ChangesetOverview?
)

data class LoaderStatus(
   val state: LoaderState,
   val message: String
) {
   companion object {
      val STARTING = LoaderStatus(LoaderState.STARTING, "Starting...")
      val OK = LoaderStatus(LoaderState.OK, "Ok")
      fun warning(message: String) = LoaderStatus(LoaderState.WARNING, message)
      fun error(message: String) = LoaderStatus(LoaderState.ERROR, message)
   }

   enum class LoaderState {
      OK,
      ERROR,
      WARNING,
      STARTING

   }
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
   /**
    * If called multiple times, the same Flux<> should be returned
    */
   fun start(): Flux<SourcePackage>
   fun loadNow(): Mono<SourcePackage>

   fun stop()

   val loaderStatus: Flux<LoaderStatus>


   val description: String

   /**
    * The root location where the sources live.
    * All returned URIs should be relative to this
    */
   val root: URI

   fun listUris(): Flux<URI>
   fun readUri(uri: URI): Mono<ByteArray>

   fun isEditable(): Boolean
   fun createChangeset(name: String): Mono<CreateChangesetResponse>

   // TODO Move these to the proper place (current module is called "publisher")
   fun addChangesToChangeset(name: String, edits: List<VersionedSource>): Mono<AddChangesToChangesetResponse>

   fun finalizeChangeset(name: String): Mono<FinalizeChangesetResponse>

   fun updateChangeset(name: String, newName: String): Mono<UpdateChangesetResponse>

   fun getAvailableChangesets(): Mono<AvailableChangesetsResponse>
   fun setActiveChangeset(branchName: String): Mono<SetActiveChangesetResponse>

   val packageIdentifier: PackageIdentifier

   val publisherType: PublisherType

   val config: ProjectTransportConfig

   /**
    * An opportunity for this loader to make changes to a ConfigSourceWriter
    * before returning it for use
    */
   fun configureWriter(writer: ConfigSourceWriter):ConfigSourceWriter {
      return writer
   }
}


/**
 * Provides an approach for converting source from one language to another.
 *
 * Outputs Taxi source code.
 *
 * This is the simpler approach for transpiling source, however
 * you lose the ability to control actual construction of the taxi document.
 *
 * Some transpiliation approaches (eg: SOAP) need the original source at execution time,
 * generally for serialization.  In this case, the SchemaSourcesAdaptor should
 * output source with a language other than Taxi, and create a SourceToTaxiConverter,
 * which outputs a converted TaxiDocument, rather than Taxi source code. This allows
 * attaching additional sources to the created services, used at execution time.
 * (See SoapWsdlSourceConverter)
 */
interface SchemaSourcesAdaptor {
   fun buildMetadata(transport: SchemaPackageTransport): Mono<PackageMetadata>
   fun convert(packageMetadata: PackageMetadata, transport: SchemaPackageTransport): Mono<SourcePackage>
}

/**
 * A lower-level interface for SchemaSourcesAdaptor.
 * Implementations are focussed purely on generating sources (generally Taxi sources)
 * from provided source files.
 *
 * They do not have to handle transport or metadata generation.
 *
 * It's common for a SchemaSourcesAdaptor to also implement this interface
 */
interface SourceGenerator {
   fun supportsSources(sourcesType: SourcesType): Boolean
   fun generateSourcePackage(sourceFiles: List<VersionedSource>, packageMetadata: PackageMetadata): SourcePackage
}

/**
 * Couldn't think of a better name.
 * Indicates a type of SchemaSourcesAdaptor which will also expose a TaxiPackageProject.
 * Often, the PackageMetadata exposed in SchemaSourcesAdaptor is sufficient.
 * However, for certain file-based operations, the full TaxiPackageProject is required
 */
interface LoaderExposingTaxiProject {
   /**
    * Returns the path of the taxi.conf file,
    * and the package project loaded from it.
    */
   fun loadTaxiProject(): Mono<Pair<Path, TaxiPackageProject>>
}

interface ProjectTransportConfig {
   /**
    * The path within the loaded project where the taxi.conf file
    * should be found. Returns the actual file path, not the directory
    */
   val pathToTaxiConf:Path
      get() {
         return Paths.get("taxi.conf")
      }
}
