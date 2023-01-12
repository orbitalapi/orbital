package io.vyne.schemaServer.schemaStoreConfig

import com.winterbe.expekt.should
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schemaServer.core.schemaStoreConfig.clustered.DistributedSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.SocketUtils

@RunWith(SpringRunner::class)
@SpringBootTest(properties = [
   "vyne.schema.server.clustered=true"
])
class ClusteredSchemaStoreTest {
   @Autowired
   private lateinit var localValidatingSchemaStoreClient: ValidatingSchemaStoreClient

   @Autowired
   private lateinit var expiringSourcesStore: ExpiringSourcesStore

   companion object {
      @JvmStatic
      @DynamicPropertySource
      fun properties(registry: DynamicPropertyRegistry) {
         registry.add("vyne.schema.server.port") { SocketUtils.findAvailableTcpPort() }
      }
   }

   @Test
   fun `Schema Server Starts With clustered SchemaSourceProvider`() {
      localValidatingSchemaStoreClient.should.be.instanceof(DistributedSchemaStoreClient::class.java)
   }
}
