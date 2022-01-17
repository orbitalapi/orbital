package io.vyne.schemaServer.schemaStoreConfig

import com.winterbe.expekt.should
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.SocketUtils

private val logger = KotlinLogging.logger {}
@RunWith(SpringRunner::class)
@SpringBootTest(properties = [
   "eureka.client.enabled=false"])
class SchemaStoreTest {
   @Autowired
   private lateinit var localValidatingSchemaStoreClient: ValidatingSchemaStoreClient

   companion object {
      @JvmStatic
      @DynamicPropertySource
      fun properties(registry: DynamicPropertyRegistry) {
         registry.add("vyne.schema.server.port") { SocketUtils.findAvailableTcpPort() }
      }
   }

   @Test
   fun `Schema Server Starts With non-clustered SchemaSourceProvider`() {
      localValidatingSchemaStoreClient.should.be.instanceof(LocalValidatingSchemaStoreClient::class.java)
   }
}
