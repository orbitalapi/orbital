package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.file.SourcesChangedMessage
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Central point for notifying repositories added / removed.
 *
 * Consumers should subscribe for updates.
 */
class ProjectStoreLifecycleManager(
) : ProjectStoreLifecycleEventSource,
   ProjectStoreLifecycleEventDispatcher,
   ProjectSpecLifecycleEventDispatcher,
   RepositorySpecLifecycleEventSource {

   private val emitFailureHandler: Sinks.EmitFailureHandler =
      Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(1000))

   private val schemaSourceRemovedSink =
      Sinks.many().replay().limit<List<PackageIdentifier>>(Duration.ofSeconds(30))
   private val schemaSourceAddedSink =
      Sinks.many().replay().limit<SchemaPackageTransport>(Duration.ofSeconds(30))
   private val gitSpecAddedSink = Sinks.many().replay().limit<GitSpecAddedEvent>(Duration.ofSeconds(30))
   private val fileSpecAddedSink = Sinks.many().replay().limit<FileSpecAddedEvent>(Duration.ofSeconds(30))
   private val gitSpecRemovedSink = Sinks.many().replay().limit<GitSpecRemovedEvent>(Duration.ofSeconds(30))
   private val fileSpecRemovedSink = Sinks.many().replay().limit<FileSpecRemovedEvent>(Duration.ofSeconds(30))

   // Replay logic here: We don't wanna keep 'em forever.
   // But, on startup, subscribers may arrive late,
   // so just keep a few seconds worth.
   private val sourcesChangedSink = Sinks.many().replay().limit<SourcesChangedMessage>(Duration.ofSeconds(30))


   override val projectStoreAdded: Flux<SchemaPackageTransport> = schemaSourceAddedSink.asFlux()

   override val sourcesChanged: Flux<SourcesChangedMessage> = sourcesChangedSink.asFlux()
   override val sourcesRemoved: Flux<List<PackageIdentifier>> = schemaSourceRemovedSink.asFlux()

   init {
      projectStoreAdded
         .subscribe { schemaTransport ->
            schemaTransport.start()
               .subscribe { sourcePackage ->
                  sourcesChangedSink.emitNext(
                     SourcesChangedMessage(listOf(sourcePackage)),
                     RetryFailOnSerializeEmitHandler
                  )
               }
         }
   }

   override val gitSpecAdded: Flux<GitSpecAddedEvent>
      get() = gitSpecAddedSink.asFlux()
   override val gitSpecRemoved: Flux<GitSpecRemovedEvent>
      get() = gitSpecRemovedSink.asFlux()
   override val fileSpecAdded: Flux<FileSpecAddedEvent>
      get() = fileSpecAddedSink.asFlux()
   override val fileSpecRemoved: Flux<FileSpecRemovedEvent>
      get() = fileSpecRemovedSink.asFlux()

   override fun fileProjectStoreAdded(repository: FileSystemPackageLoader) {
      schemaSourceAddedSink.emitOnSingleThread(repository)
   }

   override fun gitProjectStoreAdded(repository: GitSchemaPackageLoader) {
      schemaSourceAddedSink.emitOnSingleThread(repository)
   }

   override fun fileRepositorySpecAdded(spec: FileSpecAddedEvent) {
      fileSpecAddedSink.emitOnSingleThread(spec)
   }

   override fun fileRepositorySpecRemoved(spec: FileSpecRemovedEvent) {
      fileSpecRemovedSink.emitOnSingleThread(spec)
   }

   override fun gitRepositorySpecAdded(spec: GitSpecAddedEvent) {
      gitSpecAddedSink.emitOnSingleThread(spec)
   }

   override fun gitRepositorySpecRemoved(spec: GitSpecRemovedEvent) {
      gitSpecRemovedSink.emitOnSingleThread(spec)
   }

   override fun schemaSourceRemoved(packages: List<PackageIdentifier>) {
      schemaSourceRemovedSink.emitOnSingleThread(packages)
   }
}

