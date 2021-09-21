package io.vyne.queryService.schemas.editor

import mu.KotlinLogging
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}
@Configuration
class SchemaEditorConfig {

//   @Bean
//   @ConditionalOnProperty("vyne.schema.localStore")
//   fun fileBasedRepository(@Value("\${vyne.schema.localStore}") localStorePath: String): FileSystemSchemaRepository {
//      logger.info { "Schema local store is enabled, and allowing edits.  Storing at $localStorePath" }
//      TODO("Not implemented - migrating this code into the schema repository has broken dependencies, investigating")
////      return FileSystemSchemaRepository(
////         FileSystemVersionedSourceLoader(Paths.get(localStorePath))
////      )
//   }
}
