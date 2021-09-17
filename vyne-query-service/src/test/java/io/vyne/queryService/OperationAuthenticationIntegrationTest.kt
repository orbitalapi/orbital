package io.vyne.queryService

import com.google.common.io.Files
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.queryService.query.QueryService
import io.vyne.queryService.security.AuthTokenConfigurationService
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.spring.http.auth.AuthToken
import io.vyne.spring.http.auth.AuthTokenRepository
import io.vyne.spring.http.auth.AuthTokenType
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
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}"
   ]
)
class OperationAuthenticationIntegrationTest {

   @Rule
   @JvmField
   final val folder = TemporaryFolder()

   @MockBean
   lateinit var schemaProvider: SchemaSourceProvider
   @Before
   fun setup() {
      whenever(schemaProvider.schema()).thenReturn(TaxiSchema.from(
         """
            model Person {
               personId : PersonId inherits String
            }
            model Address {
               postcode : Postcode inherits String
            }
            service PersonService {
               @HttpOperation(method = "GET",url = "http://localhost:${server.port}/people")
               operation findAllPeople():Person[]
            }
            service StreetService {
               @HttpOperation(method = "GET",url = "http://localhost:${server.port}/address")
               operation findAllAddresses():Address[]
            }
         """
      ))
   }

   @Autowired
   lateinit var queryService: QueryService

   @Autowired
   lateinit var tokenService: AuthTokenConfigurationService

//   @Autowired
//   lateinit var schemaProvider: SchemaProvider

   @Rule
   @JvmField
   final val server = MockWebServerRule()

   @Test
   fun `calling a service with configured auth includes header tokens`(): Unit = runBlocking {
      tokenService.submitToken(
         "PersonService", AuthToken(
            AuthTokenType.AuthorizationBearerHeader,
            "abc123"
         )
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "personId" : "123" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""findAll { Person[] } """)
         .body.toList()
      val submittedRequest = server.takeRequest()
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION)
         .should.equal("Bearer abc123")
   }

   @Test
   fun `calling a service without configured auth does not include header tokens`(): Unit = runBlocking {
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(
            """[ { "postcode" : "SW11" } ] """
         )
      }
      val response = queryService.submitVyneQlQuery("""findAll { Address[] } """)
         .body.toList()
      val submittedRequest = server.takeRequest()
      submittedRequest.getHeader(HttpHeaders.AUTHORIZATION)
         .should.be.`null`
   }


   @TestConfiguration
   class Config {
      private val logger = KotlinLogging.logger {}

      @Bean
      @Primary
      fun tokenRepository(config: VyneHttpAuthConfig): AuthTokenRepository {
         val temporaryFolder = Files.createTempDir()
            .toPath()
         logger.info { "Creating temp folder for auth store at ${temporaryFolder.toFile().canonicalPath}" }
         return ConfigFileAuthTokenRepository(temporaryFolder.resolve("auth.conf"))
      }

      @Bean
      @Primary
      fun schemaProvider(): SimpleTaxiSchemaProvider {
         return SimpleTaxiSchemaProvider("")
      }
   }
}