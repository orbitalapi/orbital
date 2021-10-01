package io.vyne.schemaServer.publisher

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import javax.annotation.PostConstruct

@Component
class CompileOnStartupListener(
   private val sourceWatchingSchemaPublisher: SourceWatchingSchemaPublisher,
   @Value("\${vyne.schema-server.compileOnStartup:true}")  private val compileOnStartup:Boolean = true
) {

   private val logger = KotlinLogging.logger { }

   @PostConstruct
   fun handleStartup() {
      if (!this.compileOnStartup) {
         logger.info { "compileOnStartup is disabled, not performing startup compilation" }
         return
      }
      Mono.fromCallable {
         logger.info("Context refreshed, triggering a compilation")
         sourceWatchingSchemaPublisher.refreshAllSources()
      }
         .subscribeOn(Schedulers.parallel())
         .doOnError { logger.error("Could not recompile schemas", it) }
         .subscribe()
   }
}
