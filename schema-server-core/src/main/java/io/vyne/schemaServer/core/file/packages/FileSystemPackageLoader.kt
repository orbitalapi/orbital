package io.vyne.schemaServer.core.file.packages

import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.AddChangesToChangesetResponse
import io.vyne.schema.publisher.loaders.AvailableChangesetsResponse
import io.vyne.schema.publisher.loaders.CreateChangesetResponse
import io.vyne.schema.publisher.loaders.FinalizeChangesetResponse
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schema.publisher.loaders.SetActiveChangesetResponse
import io.vyne.schema.publisher.loaders.UpdateChangesetResponse
import io.vyne.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.core.publisher.toFlux
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.readBytes

class FileSystemPackageLoader(
   private val config: FileSystemPackageSpec,
   private val adaptor: SchemaSourcesAdaptor,
   private val fileMonitor: ReactiveFileSystemMonitor,
   private val eventThrottleSize: Int = 100,
   private val eventThrottleDuration: Duration = Duration.ofMillis(50),

   // Allows things like Git (which is a proxy for this)
   // to act as the decorator to the underlying transport, and
   // do things like filter out uris etc
   private val transportDecorator: SchemaPackageTransport? = null
) : SchemaPackageTransport {

   private val logger = KotlinLogging.logger {}
   private val fileEvents: Flux<List<FileSystemChangeEvent>> = fileMonitor.startWatching()

   private val sink = Sinks.many().replay().latest<SourcePackage>()

   init {
      this.fileEvents
         .bufferTimeout(eventThrottleSize, eventThrottleDuration)
         .subscribe { _ ->
            logger.info { "Received change event from file system, triggering reload of package" }
            triggerLoad()
         }
   }

   private var _packageIdentifier: PackageIdentifier? = null
   override val packageIdentifier: PackageIdentifier
      get() {
         return synchronized(this) {
            if (_packageIdentifier == null) {
               loadNow().block()
               _packageIdentifier!!
            } else {
               _packageIdentifier!!
            }
         }
      }

   private val transport: SchemaPackageTransport = transportDecorator ?: this

   private fun triggerLoad() {
      loadNow()
         .subscribe { schemaPackage ->
            logger.info { "Updated schema package ${schemaPackage.identifier} loaded.  Emitting event" }
            sink.emitNext(schemaPackage) { signalType, emitResult ->
               emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
            }
         }
   }

   fun loadNow(): Mono<SourcePackage> {
      return adaptor.buildMetadata(transport)
         .flatMap { packageMetadata ->
            adaptor.convert(packageMetadata, this)
         }.doOnNext {
            if (this._packageIdentifier == null) {
               this._packageIdentifier = it.identifier
            }
         }
   }

   /**
    * Returns the path of the taxi.conf file,
    * and the package project loaded from it.
    */
   fun loadTaxiProject(): Mono<Pair<Path, TaxiPackageProject>> {
      if (adaptor is TaxiSchemaSourcesAdaptor) {
         return adaptor.loadTaxiProject(this.transport)
      } else {
         error("Loading a taxi project is not supported if the adaptor is not a TaxiSchemaSourcesAdaptor.  (Found ${adaptor::class.simpleName}")
      }
   }

   override fun start(): Flux<SourcePackage> {
      triggerLoad()
      return sink.asFlux()
   }

   override fun listUris(): Flux<URI> {
      return config.path
         .toFile()
         .walkBottomUp()
         .map { it.toURI() }
         .toFlux()
   }

   override fun readUri(uri: URI): Mono<ByteArray> {
      return Mono.create { sink ->
         sink.success(Paths.get(uri).readBytes())
      }
   }

   override fun isEditable(): Boolean {
      return config.isEditable
   }

   override fun createChangeset(name: String): Mono<CreateChangesetResponse> {
      TODO("Not yet implemented")
   }

   override fun addChangesToChangeset(name: String, edits: List<VersionedSource>): Mono<AddChangesToChangesetResponse> {
      val writer = FileSystemPackageWriter()
      return writer.writeSources(this, edits)
         .map {
            AddChangesToChangesetResponse()
         }
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
}
