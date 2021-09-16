package io.vyne.queryService.schemas.editor

import io.vyne.schemaStore.FileSystemSchemaRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}
@Configuration
class SchemaEditorConfig {

   @Bean
   @ConditionalOnProperty("vyne.schema.localStore")
   fun fileBasedRepository(@Value("\${vyne.schema.localStore}") localStorePath: String): FileSystemSchemaRepository {
      logger.info { "Schema local store is enabled, and allowing edits.  Storing at $localStorePath" }
      return FileSystemSchemaRepository(
         Paths.get(localStorePath)
      )
   }
}
