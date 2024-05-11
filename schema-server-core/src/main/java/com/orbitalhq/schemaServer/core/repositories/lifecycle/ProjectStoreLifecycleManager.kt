package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.SourcesChangedMessage
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration

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
      Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100))

   private val schemaSourceRemovedSink = Sinks.many().replay().all<List<PackageIdentifier>>()
   private val schemaSourceAddedSink = Sinks.many().replay().all<SchemaPackageTransport>()
   private val gitSpecAddedSink = Sinks.many().replay().all<GitSpecAddedEvent>()
   private val fileSpecAddedSink = Sinks.many().replay().all<FileSpecAddedEvent>()
   private val gitSpecRemovedSink = Sinks.many().replay().all<GitSpecRemovedEvent>()
   private val fileSpecRemovedSink = Sinks.many().replay().all<FileSpecRemovedEvent>()

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
      schemaSourceAddedSink.emitNext(repository, emitFailureHandler)
   }

   override fun gitProjectStoreAdded(repository: GitSchemaPackageLoader) {
      schemaSourceAddedSink.emitNext(repository, emitFailureHandler)
   }

   override fun fileRepositorySpecAdded(spec: FileSpecAddedEvent) {
      fileSpecAddedSink.emitNext(spec, emitFailureHandler)
   }

   override fun fileRepositorySpecRemoved(spec: FileSpecRemovedEvent) {
      fileSpecRemovedSink.emitNext(spec, emitFailureHandler)
   }

   override fun gitRepositorySpecAdded(spec: GitSpecAddedEvent) {
      gitSpecAddedSink.emitNext(spec, emitFailureHandler)
   }

   override fun gitRepositorySpecRemoved(spec: GitSpecRemovedEvent) {
      gitSpecRemovedSink.emitNext(spec, emitFailureHandler)
   }

   override fun schemaSourceRemoved(packages: List<PackageIdentifier>) {
      schemaSourceRemovedSink.emitNext(packages, emitFailureHandler)
   }
}


