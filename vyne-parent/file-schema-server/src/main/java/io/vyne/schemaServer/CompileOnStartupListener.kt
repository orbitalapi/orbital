package io.vyne.schemaServer

import io.vyne.utils.log
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CompileOnStartupListener(private val compilerService:CompilerService) {
   @EventListener
   fun handleStartup(event:ContextRefreshedEvent) {
      log().info("Context refreshed, triggering a compilation")
      compilerService.recompile()
   }
}
