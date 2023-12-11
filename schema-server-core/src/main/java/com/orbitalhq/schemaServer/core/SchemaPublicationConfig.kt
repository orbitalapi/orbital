package com.orbitalhq.schemaServer.core

import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleEventSource
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
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
      eventDispatcher: ProjectStoreLifecycleEventDispatcher,
      repositoryEventSource: ProjectStoreLifecycleEventSource
   ): ReactiveProjectStoreManager {
      return ReactiveProjectStoreManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventSource, eventDispatcher, repositoryEventSource
      )
   }

   @Bean
   fun sourceWatchingSchemaPublisher(
      eventSource: ProjectStoreLifecycleEventSource,
      schemaPublisher: SchemaPublisherTransport
   ): SourceWatchingSchemaPublisher {
      return SourceWatchingSchemaPublisher(schemaPublisher, eventSource)
   }

   @Bean
   fun repositoryLifecycleManager(): ProjectStoreLifecycleManager = ProjectStoreLifecycleManager()
}

