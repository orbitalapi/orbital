package io.vyne.schemaServer.schemaStoreConfig

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisherTransport
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
import reactor.test.StepVerifier

private val logger = KotlinLogging.logger {}
@RunWith(SpringRunner::class)
@SpringBootTest(properties = [
   "eureka.client.enabled=false", "vyne.schema-server.compileOnStartup=false"])
class SchemaStoreTest {
   private val taxiSource1 = VersionedSource("test1.taxi", "1.0.0", """
         model Foo {
            bar: String
         }
      """.trimIndent())

   private val taxiSource2 = VersionedSource("test2.taxi", "1.0.0", """
         model Bar {
            foo: String
         }
      """.trimIndent())
   @Autowired
   private lateinit var localValidatingSchemaStoreClient: ValidatingSchemaStoreClient

   @Autowired
   private lateinit var schemaUpdateNotifier: SchemaUpdateNotifier

   @Autowired
   private lateinit var schemaPublisher: SchemaPublisherTransport

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

   @Test
   fun `when a source file is deleted schema change notification emitted accordingly`() {
      schemaPublisher.submitPackage(listOf(taxiSource1, taxiSource2))
      StepVerifier.create(schemaUpdateNotifier.schemaSetFlux)
         .expectNextMatches { schemaSet ->
            schemaSet.parsedPackages.map { it.name }.toSet() == setOf("test1.taxi", "test2.taxi")
         }
         .thenCancel()
         .verify()

      // pretend taxSource1 is deleted.
      schemaPublisher.submitPackage(listOf(taxiSource2))

      StepVerifier.create(schemaUpdateNotifier.schemaSetFlux)
         .expectNextMatches { schemaSet ->
            schemaSet.parsedPackages.size == 1 && schemaSet.parsedPackages.first().name == "test2.taxi"
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `when a source file is modified schema change notification emitted accordingly`() {
      schemaPublisher.submitPackage(listOf(taxiSource2))

      StepVerifier.create(schemaUpdateNotifier.schemaSetFlux)
         .expectNextMatches { schemaSet ->
            schemaSet.parsedPackages.size == 1 && schemaSet.parsedPackages.first().name == "test2.taxi"
         }
         .thenCancel()
         .verify()

      // modify the source for test2.taxi and add age field.
      schemaPublisher.submitPackage(listOf(taxiSource2.copy(content = """
         model Bar {
            foo: String
            age: Int
         }
      """.trimIndent())))

      StepVerifier.create(schemaUpdateNotifier.schemaSetFlux)
         .expectNextMatches { schemaSet ->
            schemaSet.parsedPackages.size == 1 && schemaSet.taxiSchemas.first().type("Bar").hasAttribute("age")
         }
         .thenCancel()
         .verify()
   }
}
