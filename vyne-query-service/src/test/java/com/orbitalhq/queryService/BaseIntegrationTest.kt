package com.orbitalhq.queryService

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.content.DefaultContentRepository
import com.orbitalhq.copilot.OpenAiChatService
import com.orbitalhq.licensing.LicenseManager
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.pipelines.jet.streams.HazelcastStreamResultObserver
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import org.springframework.boot.test.mock.mockito.MockBean
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

   @MockBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockBean
   lateinit var configLoader: WorkspaceConfigLoader

   @MockBean
   lateinit var hazelcastInstance: HazelcastInstance

   @MockBean
   lateinit var hazelcastStreamObserver: HazelcastStreamResultObserver

   @MockBean
   lateinit var cmsService: DefaultContentRepository

   @MockBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockBean
   lateinit var chatService: OpenAiChatService

   @MockBean
   lateinit var packagesService: PackageService

   @MockBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockBean
   lateinit var configService: ConfigService

   @MockBean
   lateinit var licenseManager: LicenseManager

   @MockBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider
}
