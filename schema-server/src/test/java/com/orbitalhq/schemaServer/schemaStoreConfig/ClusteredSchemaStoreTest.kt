package com.orbitalhq.schemaServer.schemaStoreConfig

import com.hazelcast.client.test.TestHazelcastFactory
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.schema.publisher.ExpiringSourcesStore
import com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered.DistributedSchemaStoreClient
import com.orbitalhq.schemaStore.ValidatingSchemaStoreClient
import com.winterbe.expekt.should
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.util.TestSocketUtils

@RunWith(SpringRunner::class)
@SpringBootTest(properties = [
   "vyne.schema.server.clustered=true"
])
class ClusteredSchemaStoreTest {
   @TestConfiguration
   open class SpringConfig {
      @Bean
      open fun hazelcastInstance(): HazelcastInstance = TestHazelcastFactory().newHazelcastInstance()
   }
   @Autowired
   private lateinit var localValidatingSchemaStoreClient: ValidatingSchemaStoreClient

   @Autowired
   private lateinit var expiringSourcesStore: ExpiringSourcesStore

   companion object {
      @JvmStatic
      @DynamicPropertySource
      fun properties(registry: DynamicPropertyRegistry) {
         registry.add("vyne.schema.server.port") { TestSocketUtils.findAvailableTcpPort() }
      }
   }

   @Test
   fun `Schema Server Starts With clustered SchemaSourceProvider`() {
      localValidatingSchemaStoreClient.should.be.instanceof(DistributedSchemaStoreClient::class.java)
   }
}
