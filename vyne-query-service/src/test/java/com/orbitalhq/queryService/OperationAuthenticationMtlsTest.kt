package com.orbitalhq.queryService

import app.cash.turbine.withTurbineTimeout
import com.google.common.io.Files
import com.orbitalhq.auth.schemes.MutualTls
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.security.AuthTokenConfigurationService
import com.orbitalhq.copilot.OpenAiChatService
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.queryService.JavaKeyStore.Companion.createCertificates
import com.orbitalhq.queryService.JavaKeyStore.Companion.createPrivateKey
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.spring.http.auth.VyneHttpAuthConfig
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.Transferable
import reactor.test.StepVerifier
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.seconds


@ExtendWith(SpringExtension::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.telemetry.enabled=false",
      "vyne.search.directory=./search/\${random.int}"
   ]
)
@ActiveProfiles("test")
class OperationAuthenticationMtlsTest : DatabaseTest() {

   companion object {
      @JvmStatic
      @TempDir
      lateinit var folder: Path

      @JvmStatic
      @DynamicPropertySource
      fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
         registry.add("vyne.auth.config-file") {
            Files.write("""
                  authenticationTokens {
                  }
               """.trimIndent().encodeToByteArray(), folder.resolve("auth.conf").toFile())
            folder.resolve("auth.conf").absolutePathString()
         }
      }

      val mtlsSideCarProxy = MtlsSideCarProxy()
         .withExposedPorts(443)
         .withCopyToContainer(Transferable.of(ClassPathResource("/mtls/server-certs/clients-ca.pem")
            .getContentAsString(Charsets.UTF_8)), "/etc/nginx/conf.d/certs/clients-ca.pem")
         .withCopyToContainer(Transferable.of(ClassPathResource("/mtls/server-certs/server.pem")
            .getContentAsString(Charsets.UTF_8)), "/etc/nginx/conf.d/certs/server.pem")
         .withCopyToContainer(Transferable.of(ClassPathResource("/mtls/server-certs/server-ca.pem")
            .getContentAsString(Charsets.UTF_8)), "/etc/nginx/conf.d/certs/server-ca.pem")
         .withCopyToContainer(Transferable.of(ClassPathResource("/mtls/server-certs/server-key.pem")
            .getContentAsString(Charsets.UTF_8)), "/etc/nginx/conf.d/certs/server-key.pem")
         .withEnv("ALLOWED_CERTIFICATE_FINGERPRINT", "all")

         .let {
            it.start()
            it.waitingFor(Wait.defaultWaitStrategy())
            it
         }
   }

   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class TestConfig {
      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider {
         val source = """
          model Todo {
               title : PersonId inherits String
               description: PersonName inherits String
            }

            model Assignee {
               name: AssigneeName inherits String
            }

            service AssigneeService {
               @HttpOperation(method = "GET",url = "https://${mtlsSideCarProxy.host}:${mtlsSideCarProxy.firstMappedPort}/assignee")
               operation findAllAssignees(): Assignee[]
            }
            service ToDoService {
               @HttpOperation(method = "GET",url = "https://${mtlsSideCarProxy.host}:${mtlsSideCarProxy.firstMappedPort}/todo")
               operation findAllTodos(): Todo[]


            }
      """.trimIndent()
         val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")
         return  TestSchemaProvider.withBuiltInsAnd(schema.sources)
      }

      @Bean
      fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()


   }
   private lateinit var taxiSchema: TaxiSchema

   @MockBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager


   @MockBean
   lateinit var queryMetricsReporter: QueryMetricsReporter

   @MockBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockBean
   lateinit var configLoader : WorkspaceConfigLoader

   @MockBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   @MockBean
   lateinit var packagesService: PackageService

   @MockBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockBean
   lateinit var chatService: OpenAiChatService

   @Bean
   fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()

   @Autowired
   lateinit var queryService: QueryService

   @Autowired
   lateinit var tokenService: AuthTokenConfigurationService

   // happens when -> "vyne.schema.publicationMethod=LOCAL"
   @Autowired
   lateinit var localValidatingSchemaStoreClient: LocalValidatingSchemaStoreClient


   private lateinit var keyStore: JavaKeyStore
   private lateinit var trustStore: JavaKeyStore

   private val password = "orbital"
   fun setUpOrbitalMtls() {
      val keyStorePath =  folder.resolve("mtls-keystore.jks").absolutePathString()
      keyStore = JavaKeyStore(keyStorePassword = "orbital", keyStorePath = keyStorePath)
      val trustStorePath =  folder.resolve("mtls-truststore.jks").absolutePathString()
      trustStore = JavaKeyStore(keyStorePassword = "orbital", keyStorePath = trustStorePath)

      keyStore.createEmptyKeyStore()
      keyStore.loadKeyStore()
      trustStore.createEmptyKeyStore()
      trustStore.loadKeyStore()
      val clientCertificateChain =  createCertificates(ClassPathResource("mtls/client-certs/client.pem"))!!

      val privateKey = createPrivateKey(ClassPathResource("mtls/client-certs/client-key.pem"))
      keyStore.setKeyEntry("private_key", privateKey, password, clientCertificateChain)

      val serverCertificateChain = createCertificates(ClassPathResource("mtls/client-certs/server-ca.pem"))!!
      trustStore.setCertificateEntry("server_ca", serverCertificateChain[0])

      trustStore.getCertificate("server_ca").publicKey.should.not.be.`null`
      trustStore.persist()
      keyStore.persist()
   }

   @Test
   fun canInvokeExternalApiWithSsl(): Unit = runBlocking {
      setUpOrbitalMtls()
      val token = MutualTls(
         keystorePath = folder.resolve("mtls-keystore.jks").absolutePathString(),
         keystorePassword = password,
         truststorePath = folder.resolve("mtls-truststore.jks").absolutePathString(),
         truststorePassword = password
      )
      tokenService.submitAuthScheme(
         VyneHttpAuthConfig.PACKAGE_IDENTIFIER.uriSafeId,
         "ToDoService", token
      ).block()

      val response = queryService.submitVyneQlQuery("""find {  Todo[] }""")
         .block()
         .body!!.toList()

      response.size.should.equal(2)
   }

   @Test
   fun `invoking an mtls end point without ssl results in error`(): Unit = runTest {
      withTurbineTimeout(20.seconds) {
         val response = queryService
            .submitVyneQlQuery("""find { Assignee[] }""")
            .block()?.body

         StepVerifier.create(response).expectError()
         //response!!.awaitError()
      }
   }

}

class MtlsSideCarProxy: GenericContainer<MtlsSideCarProxy>(ImageFromDockerfile()
   .withFileFromClasspath("nginx.conf", "mtls/nginx/nginx.conf")
   .withFileFromClasspath("Dockerfile", "mtls/nginx/Dockerfile"))

