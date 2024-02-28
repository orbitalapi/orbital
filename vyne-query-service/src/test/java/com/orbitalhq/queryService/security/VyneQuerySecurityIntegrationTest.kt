package com.orbitalhq.queryService.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.winterbe.expekt.should
import io.kotest.matchers.booleans.shouldBeTrue
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.queryService.TestSchemaProvider
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import org.jose4j.jwk.RsaJsonWebKey
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import reactivefeign.utils.HttpStatus

/**
 *
 *
 * --vyne.security.openIdp.enabled=true
--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/auth/realms/Vyne/protocol/openid-connect/certs
--vyne.security.openIdp.issuerUrl=http://localhost:8080/auth/realms/Vyne
--vyne.security.openIdp.clientId=vyne-spa
--vyne.security.openIdp.scope=openid
profile
 */
@RunWith(SpringRunner::class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "spring.datasource.url=jdbc:h2:mem:testdbVyneQuerySecureIntegrationTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY",
      "vyne.security.open-idp.jwks-uri=\${wiremock.server.baseUrl}/.well-known/jwks.json",
      "vyne.security.openIdp.enabled=true",
      "vyne.security.open-idp.issuer-url=http://localhost:\${wiremock.server.port}",
      "vyne.security.open-idp.client-id=vyne-spa",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
      "vyne.analytics.persistResults=true",
      "vyne.telemetry.enabled=false",
   ]
)
class VyneQuerySecurityIntegrationTest {

   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

   }

   private var rsaJsonWebKey: RsaJsonWebKey? = null
   private var jwsBuilder: JWSBuilder? = null

   @Value("\${wiremock.server.baseUrl}")
   private lateinit var wireMockServerBaseUrl: String

   @MockBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager


   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @Autowired
   private lateinit var objectMapper: ObjectMapper

   @MockBean
   lateinit var queryMetricsReporter: QueryMetricsReporter

   @MockBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   /**
    * see "authorisation/user-role-mappings.conf" in resources.
    */
   private val adminUserName = "adminUser"
   private val platformManagerUser = "platformManager"
   private val queryRunnerUser = "queryExecutor"
   private val viewerUserName = "viewer"

   private val roles = mapOf(
      adminUserName to listOf("Admin"),
      platformManagerUser to listOf("PlatformManager"),
      queryRunnerUser to listOf("QueryRunner"),
      viewerUserName to listOf("Viewer"),
      "userWithoutAnyRoleSetup" to emptyList()
   )


   @MockBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockBean
   lateinit var configLoader : WorkspaceConfigLoader


   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class TestVyneAuthorisationConfig {
      @Bean
      @Primary
      fun schemaProvider( @Value("\${wiremock.server.baseUrl}") mockServerBaseUrl: String): SchemaProvider {
         val source = """
         namespace com.orbitalhq.queryService {
           type AccountId inherits String
           type ContactId inherits String

           [[ Custom JwtClaim model which inherits from Orbital's JwtClaim base ]]
           model CompanyXJwtClaim inherits JwtClaim {
              contactId: ContactId
              accountId: AccountId
            }

           type AccountName inherits String
           model Account {
               accountName : AccountName
               accountId : AccountId
            }

           [[ A service the defines an opeation that can be invoked from accountId value contained in currently logged in user Jwt claims ]]
            service AccountService {
               @HttpOperation(method = "GET",url = "$mockServerBaseUrl/account/{accountId}")
               operation getByAccountId(@PathVariable("accountId") accountId: AccountId): Account
            }
       }


      """.trimIndent()
         val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")
        return  TestSchemaProvider.withBuiltInsAnd(schema.sources)
      }
      @Bean
      fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()

      @Primary
      @Bean
      fun vyneAuthorisationConfig(): VyneAuthorisationConfig {
         return VyneAuthorisationConfig()
      }
   }

   @Test
   fun `unauthenticated user can not can not execute query`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = issueVyneQuery(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   @Test
   fun `a viewer user can not execute query`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a user without Query Runner role can not execute query`() {
      val token = setUpLoggedInUser("userWithoutAnyRoleSetup")
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a query runner can execute query`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCode.is2xxSuccessful.shouldBeTrue()
   }

   @Test
   fun `a platform manager can not execute query`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `an admin user can execute query`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCode.is2xxSuccessful.shouldBeTrue()
   }

   /**
    * Start get historical query list
    */
   @Test
   fun `an admin user can see query history list`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a platform manager can see query history list`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a query runner can not see query history list`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a viewer user can not see query history list`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `unauthenticated user can not see query history list`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = listQueryHistory(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   /**
    * End get historical query list
    */

   /**
    * Start get historical query result
    */
   @Test
   fun `an admin user can see query result`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a platform manager can see query result`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a query runner can not see query result`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }


   @Test
   fun `a viewer user can not query result`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   /**
    * End get historical query result
    */



   @Test
   fun `a query runner can not get pipelines`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a viewer user can not get pipelines`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `unauthenticated user can not get pipelines`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getPipelines(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   /**
    * End Get Pipelines
    */

   /**
    * Get Authentication Tokens
    */
   @Test
   @Ignore
   fun `an admin user can get authentication tokens`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   @Ignore
   fun `a platform manager can get authentication tokens`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a query runner can not can get authentication tokens`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a viewer user can not get authentication tokens`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `unauthenticated user can not get authentication tokens`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getAuthenticationTokens(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   /**
    * End Authentication Tokens
    */

   /**
    * Delete Authentication Token
    */
   @Test
   @Ignore("removing ability to edit tokens from UI")
   fun `an admin user can delete authentication tokens`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   @Ignore("removing ability to edit tokens from UI")
   fun `a platform manager can delete authentication tokens`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   @Ignore("removing ability to edit tokens from UI")
   fun `a query runner can not delete authentication tokens`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   @Ignore("removing ability to edit tokens from UI")
   fun `a viewer user can not delete authentication tokens`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   @Ignore("removing ability to edit tokens from UI")
   fun `unauthenticated user can not delete authentication tokens`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = deleteAuthenticationToken(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   /**
    * End Delete Authentication Tokens
    */

   /**
    * Get jdbc connections
    */
   @Test
   fun `an admin user can get jdbc connections`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a platform manager can get jdbc connections`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   fun `a query runner can not get jdbc connections`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `a viewer user can not get jdbc connections`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `unauthenticated user can not get jdbc connections`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getJdbcConnections(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   /**
    * End Get Jdbc Connections.
    */

   /**
    * Create jdbc connection
    */
   @Test
   @Ignore("creating connections from UI is disabled for now")
   fun `an admin user can create jdbc connections`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   @Ignore("creating connections from UI is disabled for now")
   fun `a platform manager can create jdbc connections`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_OK))
   }

   @Test
   @Ignore("creating connections from UI is disabled for now")
   fun `a query runner can not create jdbc connections`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   @Ignore("creating connections from UI is disabled for now")
   fun `a viewer user can not create jdbc connections`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_FORBIDDEN))
   }

   @Test
   fun `unauthenticated user can not create jdbc connections`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = createJdbcConnection(headers)
      response.statusCode.should.be.equal(HttpStatusCode.valueOf(HttpStatus.SC_UNAUTHORIZED))
   }

   @Test
   fun `Claims Can be injected to Query Execution as custom model`() {
      val accountId = "456"
      val token = setUpLoggedInUser(queryRunnerUser,
         mapOf(
            "contactId" to "123",
            "accountId" to accountId
            )
      )

      // Prepare the WireMock to respond for:
      // @HttpOperation(method = "GET",url = "$mockServerBaseUrl/account/{accountId}")
      WireMock.stubFor(
         WireMock.get(WireMock.urlEqualTo("/account/$accountId"))
            .willReturn(
               WireMock.aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("""
                     {
                       "accountName" : "accountFoo",
                       "accountId" : "$accountId"
                      }
                  """.trimIndent())
            )
      )
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val query = """
         query MyQuery ( jwt:CompanyXJwtClaim) {
           given { jwt }
           find { com.orbitalhq.queryService.Account }
         }
      """.trimIndent()


      val response = issueVyneQuery(headers, query)
      response.statusCode.is2xxSuccessful.shouldBeTrue()
      response.body.should.equal("""
         {"accountName":"accountFoo","accountId":"456"}
      """.trimIndent())
   }

   /**
    * End Create Jdbc Connection
    */

   private fun setUpLoggedInUser(userName: String, customClaims: Map<String, Any> = emptyMap()): String {
      val setUpIdpJwt = JWSBuilder.setUpRsaJsonWebKey(userName)
      this.jwsBuilder = setUpIdpJwt.first
      this.rsaJsonWebKey = setUpIdpJwt.second
      JWSBuilder.initialiseIdpServer(wireMockServerBaseUrl, this.jwsBuilder!!, this.rsaJsonWebKey!!)
      return jwsBuilder!!.build(roles[userName]!!, customClaims).compactSerialization
   }

   private fun issueVyneQuery(headers: HttpHeaders, query: String = "find { com.orbitalhq.Username[] }"): ResponseEntity<String> {
      val entity = HttpEntity(query, headers)
      return restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
   }

   private fun listQueryHistory(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange("/api/query/history", HttpMethod.GET, entity, String::class.java)
   }

   private fun getQueryResult(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(
         "/api/query/history/summary/clientId/foo",
         HttpMethod.GET,
         entity,
         String::class.java
      )
   }

   private fun getPipelines(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getPipelines, HttpMethod.GET, entity, String::class.java)
   }

   private fun deletePipeline(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.deletePipelines, HttpMethod.DELETE, entity, String::class.java)
   }

   private fun getAuthenticationTokens(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getAuthenticationTokens, HttpMethod.GET, entity, String::class.java)
   }

   private fun deleteAuthenticationToken(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.deleteAuthenticationToken, HttpMethod.DELETE, entity, String::class.java)
   }

   private fun getJdbcConnections(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getJdbcConnections, HttpMethod.GET, entity, String::class.java)
   }

   private fun createJdbcConnection(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity(
         DefaultJdbcConnectionConfiguration(
            "foo", JdbcDriver.H2, mapOf(
               Pair("catalog", "dsda"), Pair("username", "foo"), Pair("password", "bar")
            )
         ),
         headers
      )
      return restTemplate.exchange(JWSBuilder.createJdbcConnection, HttpMethod.POST, entity, String::class.java)
   }

   private fun getUserRoleDefinitions(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getUserRoleDefinitions, HttpMethod.GET, entity, String::class.java)

   }
}


