package io.vyne.schemaServer.core

import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaServer.core.repositories.lifecycle.*
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SchemaPublicationConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun repositoryManager(
      eventSource: RepositorySpecLifecycleEventSource,
      eventDispatcher: RepositoryLifecycleEventDispatcher
   ): ReactiveRepositoryManager {
      return ReactiveRepositoryManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventSource, eventDispatcher
      )
   }

   @Bean
   fun sourceWatchingSchemaPublisher(
      eventSource: RepositoryLifecycleEventSource,
      schemaPublisher: SchemaPublisherTransport
   ): SourceWatchingSchemaPublisher {
      return SourceWatchingSchemaPublisher(schemaPublisher, eventSource)
   }

   @Bean
   fun repositoryLifecycleManager(): RepositoryLifecycleManager = RepositoryLifecycleManager()
}

