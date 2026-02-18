package com.orbitalhq.queryService

import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.content.DefaultContentRepository
import com.orbitalhq.copilot.CopilotConversationApi
import com.orbitalhq.licensing.OrbitalLicenseManager
import com.orbitalhq.pipelines.jet.streams.HazelcastStreamResultObserver
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class BaseIntegrationTest {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>
   }

   @MockitoBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockitoBean
   lateinit var configLoader: WorkspaceConfigLoader


   @MockitoBean
   lateinit var hazelcastStreamObserver: HazelcastStreamResultObserver

   @MockitoBean
   lateinit var cmsService: DefaultContentRepository

   @MockitoBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockitoBean
   lateinit var chatService: CopilotConversationApi

   @MockitoBean
   lateinit var packagesService: PackageService

   @MockitoBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockitoBean
   lateinit var configService: ConfigService

   @MockitoBean
   lateinit var licenseManager: OrbitalLicenseManager

   @MockitoBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider
}
