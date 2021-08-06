package io.vyne.schemaServer

import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import javax.annotation.PostConstruct

class CompileOnStartupListener(
   private val versionedSourceLoaders: List<VersionedSourceLoader>,
   private val compilerService: CompilerService,
) {

   private val logger = KotlinLogging.logger { }

   @PostConstruct
   fun handleStartup() {
      Mono.fromCallable {
            logger.info("Context refreshed, triggering a compilation")
            val sources = versionedSourceLoaders.flatMap { it.loadVersionedSources(incrementVersion = true) }
            compilerService.recompile(sources)
         }
         .subscribeOn(Schedulers.parallel())
         .doOnError { logger.error("Could not recompile schemas", it) }
         .subscribe()
   }
}
