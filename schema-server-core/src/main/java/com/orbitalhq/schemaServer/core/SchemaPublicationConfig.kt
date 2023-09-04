package com.orbitalhq.schemaServer.core

import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositoryLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositoryLifecycleEventSource
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventSource
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SchemaPublicationConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun repositoryManager(
      eventSource: RepositorySpecLifecycleEventSource,
      eventDispatcher: RepositoryLifecycleEventDispatcher,
      repositoryEventSource: RepositoryLifecycleEventSource
   ): ReactiveRepositoryManager {
      return ReactiveRepositoryManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventSource, eventDispatcher, repositoryEventSource
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

