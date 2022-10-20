package io.vyne.schemaServer.core

import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemaServer.core.file.FileSystemSchemaRepository
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositorySourceLoader
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiVersionedSourceLoader
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SchemaPublicationConfig {

   private val logger = KotlinLogging.logger {}

   // TODO : We will eventually need to defer all this stuff once we allow changing
   // repo config at runtime (ie., via a rest service)

   @Bean
   fun gitConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): GitSchemaRepositoryConfig {
      return loader.load().git ?: GitSchemaRepositoryConfig()
   }

   @Bean
   fun openApiConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): OpenApiSchemaRepositoryConfig {
      return loader.load().openApi ?: OpenApiSchemaRepositoryConfig()
   }

   @Bean
   fun fileConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): FileSystemSchemaRepositoryConfig {
      return loader.load().file ?: FileSystemSchemaRepositoryConfig()
   }

   @Bean
   fun fileSchemaChangePublisher(
      openApiVersionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
      gitRepositories: List<GitRepositorySourceLoader>,
      fileRepositories: List<FileSystemSchemaRepository>,
      schemaPublisher: SchemaPublisherTransport
   ): SourceWatchingSchemaPublisher {
      val loaders: List<VersionedSourceLoader> = openApiVersionedSourceLoaders + gitRepositories + fileRepositories
      logger.info {"Detected ${loaders.size} total loaders - ${openApiVersionedSourceLoaders.size} openApi loaders, ${gitRepositories.size} gitRepository loaders, ${fileRepositories.size} fileRepository loaders"  }
      return SourceWatchingSchemaPublisher(loaders, schemaPublisher)
   }
}

