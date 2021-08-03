package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import javax.annotation.PostConstruct

@Component
class CompileOnStartupListener(private val localFileSchemaPublisherBridge:LocalFileSchemaPublisherBridge) {
   @PostConstruct
   fun handleStartup() {
      Mono.fromCallable {
            log().info("Context refreshed, triggering a compilation")
            localFileSchemaPublisherBridge.rebuildSourceList()
         }
         .subscribeOn(Schedulers.parallel())
         .doOnError { log().error("Could not recompile schemas", it) }
         .subscribe()
   }
}
