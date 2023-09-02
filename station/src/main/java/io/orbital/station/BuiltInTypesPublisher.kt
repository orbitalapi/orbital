package io.orbital.station

import arrow.core.Either
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import com.orbitalhq.schemaServer.core.packages.SchemaServerSourceManager
import com.orbitalhq.schemas.taxi.toMessage
import lang.taxi.errors
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class BuiltInTypesPublisher(sourceManager: SchemaServerSourceManager) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Submitting built-in types" }
      val result = sourceManager.submitPackage(BuiltInTypesProvider.sourcePackage)
      when (result) {
         is Either.Left -> {
            logger.warn { "Failed to publish built in types: ${result.value.errors.errors().toMessage()}" }
         }

         is Either.Right -> {
            logger.info { "Built in types published successfully" }
         }
      }
   }
}
