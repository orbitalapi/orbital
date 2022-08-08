package io.vyne.schemaServer.core.file.packages

import io.vyne.schema.api.PackageMetadata
import io.vyne.schema.api.SchemaPackage
import io.vyne.schema.publisher.loaders.SchemaPackageTransport
import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.core.publisher.toFlux
import java.net.URI
import java.time.Duration

class FileSystemPackageLoader(
   private val config: FileSystemPackageSpec,
   private val adaptor: SchemaSourcesAdaptor,
   private val fileMontitor: ReactiveFileSystemMonitor,
   private val eventThrottleSize: Int = 100,
   private val eventThrottleDuration: Duration = Duration.ofMillis(50)
) : SchemaPackageTransport {

   private val logger = KotlinLogging.logger {}
   private val fileEvents: Flux<List<FileSystemChangeEvent>>

   private val sink = Sinks.many().replay().latest<SchemaPackage>()

   init {
      this.fileEvents = fileMontitor.startWatching()
      this.fileEvents
         .bufferTimeout(eventThrottleSize, eventThrottleDuration)
         .subscribe { changeEvent ->
            logger.info { "Received change event from file system, triggering reload of package" }
            triggerLoad()
         }

   }

   private fun triggerLoad() {
      adaptor.buildMetadata(this)
         .flatMap { packageMetadata ->
            adaptor.convert(packageMetadata, this)
         }
         .subscribe { schemaPackage ->
            logger.info { "Updated schema package ${schemaPackage.identifier} loaded.  Emitting event" }
            sink.emitNext(schemaPackage, Sinks.EmitFailureHandler.FAIL_FAST)
         }
   }

   override fun start(): Flux<SchemaPackage> {
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
      TODO("Not yet implemented")
   }
}
