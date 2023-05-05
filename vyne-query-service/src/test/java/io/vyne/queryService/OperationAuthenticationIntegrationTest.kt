package io.vyne.queryService

import com.google.common.io.Files
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.asPackage
import io.vyne.auth.tokens.AuthToken
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.auth.tokens.AuthTokenType
import io.vyne.cockpit.core.security.AuthTokenConfigurationService
import io.vyne.http.MockWebServerRule
import io.vyne.query.runtime.core.QueryService
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.config.TestDiscoveryClientConfig
import io.vyne.spring.http.auth.ConfigFileAuthTokenRepository
import io.vyne.spring.http.auth.VyneHttpAuthConfig
import kotlinx.coroutines.flow.toList
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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
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
class OperationAuthenticationIntegrationTest {
   private lateinit var taxiSchema: TaxiSchema

   @Rule
   @JvmField
   final val folder = TemporaryFolder()

   @MockBean
   lateinit var schemaProvider: SchemaProvider

   @Before
   fun setup() {
      taxiSchema =  TaxiSchema.from(
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
      )
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
      val token = AuthToken(
         tokenType = AuthTokenType.QueryParam,
         value = "abc123",
         paramName = "api_key"
      )
      tokenService.submitToken(
         "PersonFindByIdService", token
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """{ "personId" : "123", "personName": "foo" }"""
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person(PersonId == "123") }""")
         .body!!.toList()
      response.should.not.be.`null`
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION).should.be.`null`
      submittedRequest.requestUrl!!.query.should.equal("id=123&api_key=abc123")
   }

   @Test
   fun `calling a service with configured auth includes header tokens`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = AuthToken(
         tokenType = AuthTokenType.Header,
         value = "abc123",
         paramName = "Authorization",
         valuePrefix = "Bearer"
      )
      tokenService.submitToken(
         "PersonService", token
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION)
         .should.equal("Bearer abc123")
   }

   @Test
   fun `calling a service with configured query param auth includes query param name values`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = AuthToken(
         tokenType = AuthTokenType.QueryParam,
         value = "abc123",
         paramName = "api_key"
      )
      tokenService.submitToken(
         "PersonService", token
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
         .body!!.toList()
      val submittedRequest = server.takeRequest(10L)
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION).should.be.`null`
      submittedRequest.requestUrl!!.query.should.equal("api_key=abc123")
   }

   @Test
   fun `calling a service with cookie auth includes relevant cookie values`(): Unit = runBlocking {
      localValidatingSchemaStoreClient.submitPackage(taxiSchema.sources.asPackage())
      val token = AuthToken(
         tokenType = AuthTokenType.Cookie,
         value = "abc123",
         paramName = "api_key"
      )
      tokenService.submitToken(
         "PersonService", token
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }

      val response = queryService.submitVyneQlQuery("""find { Person[] } """)
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
      @Primary
      fun schemaProvider(): SchemaProvider = SimpleTaxiSchemaProvider(VyneQueryIntegrationTest.UserSchema.source)

      @Bean
      fun schemaStore():SchemaStore = LocalValidatingSchemaStoreClient()


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
