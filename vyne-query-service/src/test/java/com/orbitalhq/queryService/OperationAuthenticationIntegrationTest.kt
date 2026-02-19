package com.orbitalhq.queryService

import com.google.common.io.Files
import com.hazelcast.core.HazelcastInstance
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.asPackage
import com.orbitalhq.auth.schemes.Cookie
import com.orbitalhq.auth.schemes.HttpHeader
import com.orbitalhq.auth.schemes.QueryParam
import com.orbitalhq.auth.tokens.AuthTokenRepository
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.content.DefaultContentRepository
import com.orbitalhq.cockpit.core.security.AuthTokenConfigurationService
import com.orbitalhq.copilot.CopilotConversationApi
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.licensing.LicenseManager
import com.orbitalhq.licensing.OrbitalLicenseManager
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.spring.SimpleTaxiSchemaProvider
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.spring.http.auth.ConfigFileAuthTokenRepository
import com.orbitalhq.spring.http.auth.VyneHttpAuthConfig
import com.orbitalhq.withBuiltIns
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
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
class OperationAuthenticationIntegrationTest : DatabaseTest() {
   private lateinit var taxiSchema: TaxiSchema

   @MockitoBean
   lateinit var streamResultStreamProvider: StreamResultStreamProvider

   @MockitoBean
   lateinit var chatService: CopilotConversationApi

   @MockitoBean
   lateinit var cmsService: DefaultContentRepository


   @MockitoBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockitoBean
   lateinit var packagesService: PackageService

   @MockitoBean
   lateinit var schemaEditorService: SchemaEditorService

   @MockitoBean
   lateinit var queryMetricsReporter: QueryMetricsReporter

   @MockitoBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockitoBean
   lateinit var configLoader : WorkspaceConfigLoader

   @MockitoBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   @MockitoBean
   lateinit var configService: ConfigService
   @MockitoBean
   lateinit var licenseManager: OrbitalLicenseManager


   @Rule
   @JvmField
   final val folder = TemporaryFolder()

   @MockitoBean
   lateinit var schemaProvider: SchemaProvider

   @Before
   fun setup() {
      taxiSchema = TaxiSchema.from(
         """
            model Person {
               personId : PersonId inherits String
               personName: PersonName inherits String
            }
            model Address {
               postcode : Postcode inherits String
            }
            service PersonService {
               @HttpOperation(method = "GET",url = "http://localhost:${server.port}/people")
               operation findAllPersons(): Person[]
            }

            service PersonFindByIdService {
               @HttpOperation(method = "GET",url = "http://localhost:${server.port}/people?id={id}")
               operation findById(@RequestParam("id") id: PersonId):Person (PersonId == id)
            }
            service StreetService {
               @HttpOperation(method = "GET",url = "http://localhost:${server.port}/address")
               operation findAllAddresses():Address[]
            }
         """
      ).withBuiltIns()
      whenever(schemaProvider.schema).thenReturn(taxiSchema)
   }

   @Autowired
   lateinit var queryService: QueryService

   @Autowired
   lateinit var tokenService: AuthTokenConfigurationService

   // happens when -> "vyne.schema.publicationMethod=LOCAL"
   @Autowired
   lateinit var localValidatingSchemaStoreClient: LocalValidatingSchemaStoreClient

   @Rule
   @JvmField
   final val server = MockWebServerRule()

   @Test
   fun `calling an operation with configured query param auth includes query param name values`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = QueryParam(
         value = "abc123",
         parameterName = "api_key"
      )
      tokenService.submitAuthScheme(
         VyneHttpAuthConfig.PACKAGE_IDENTIFIER.uriSafeId,
         "PersonFindByIdService", token
      ).block()
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """{ "personId" : "123", "personName": "foo" }"""
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person(PersonId == "123") }""")
         .block()!!
         .body!!.single()
      response.should.not.be.`null`
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION).should.be.`null`
      submittedRequest.requestUrl!!.query.should.equal("id=123&api_key=abc123")
   }

   @Test
   fun `calling a service with configured auth includes header tokens`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val authScheme = HttpHeader(
         value = "abc123",
         headerName = "Authorization",
         prefix = "Bearer"
      )
      tokenService.submitAuthScheme(
         VyneHttpAuthConfig.PACKAGE_IDENTIFIER.uriSafeId,
         "PersonService", authScheme
      ).block()


      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
         .block()!!
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION)
         .should.equal("Bearer abc123")
   }

   @Test
   fun `calling a service with configured query param auth includes query param name values`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = QueryParam(
         value = "abc123",
         parameterName = "api_key"
      )
      tokenService.submitAuthScheme(
         VyneHttpAuthConfig.PACKAGE_IDENTIFIER.uriSafeId,
         "PersonService", token
      ).block()
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
         .block()!!
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION).should.be.`null`
      submittedRequest.requestUrl!!.query.should.equal("api_key=abc123")
   }

   @Test
   fun `calling a service with cookie auth includes relevant cookie values`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = Cookie(
         value = "abc123",
         cookieName = "api_key",
      )
      tokenService.submitAuthScheme(
         VyneHttpAuthConfig.PACKAGE_IDENTIFIER.uriSafeId,
         "PersonService", token
      ).block()

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }

      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
         .block()!!
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.COOKIE)
         .should.equal("api_key=abc123")
   }

   @Test
   fun `calling a service without configured auth does not include header tokens`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "postcode" : "SW11" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Address[] } """)
         .block()!!
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION)
         .should.be.`null`
   }


   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class Config {
      private val logger = KotlinLogging.logger {}

      @Bean
      fun hazelcastInstance(): HazelcastInstance = MockHazelcastInstance()


      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider = SimpleTaxiSchemaProvider(VyneQueryIntegrationTest.UserSchema.source)

      @Bean
      fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()


      @Bean
      @Primary
      fun tokenRepository(config: VyneHttpAuthConfig): AuthTokenRepository {
         val temporaryFolder = Files.createTempDir()
            .toPath()
         logger.info { "Creating temp folder for auth store at ${temporaryFolder.toFile().canonicalPath}" }
         return ConfigFileAuthTokenRepository(temporaryFolder.resolve("auth.conf"))
      }
   }
}
