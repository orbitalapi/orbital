package io.vyne.schemaServer

import io.vyne.schemaServer.file.FileChangeSchemaPublisher
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import javax.annotation.PostConstruct

class CompileOnStartupListener(
   private val versionedSourceLoaders: List<VersionedSourceLoader>,
   private val fileChangeSchemaPublisher: FileChangeSchemaPublisher
) {

   private val logger = KotlinLogging.logger { }

   @PostConstruct
   fun handleStartup() {
      Mono.fromCallable {
         logger.info("Context refreshed, triggering a compilation")
         val sources = versionedSourceLoaders.associate {
            it.identifier to it.loadVersionedSources(forceVersionIncrement = true)
         }
         fileChangeSchemaPublisher.refreshAllSources()
//            compilerService.recompile(sources)
      }
         .subscribeOn(Schedulers.parallel())
         .doOnError { logger.error("Could not recompile schemas", it) }
         .subscribe()
   }
}
