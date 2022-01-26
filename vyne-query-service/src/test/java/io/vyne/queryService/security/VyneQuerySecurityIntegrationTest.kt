package io.vyne.queryService.security

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.queryService.security.authorisation.VyneAuthorisationConfig
import org.jose4j.jwk.RsaJsonWebKey
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import reactivefeign.utils.HttpStatus

@RunWith(SpringRunner::class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("secure")
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}",
      "spring.datasource.url=jdbc:h2:mem:testdbVyneQuerySecureIntegrationTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=\${wiremock.server.baseUrl}/.well-known/jwks.json",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
      "vyne.analytics.persistResults=true",
      "vyne.caskService.url=http://localhost:\${wiremock.server.port}",
      "vyne.pipelinesJetRunner.url=http://localhost:\${wiremock.server.port}"
   ])
class VyneQuerySecurityIntegrationTest {

   private var rsaJsonWebKey: RsaJsonWebKey? = null
   private var jwsBuilder: JWSBuilder? = null

   @Value("\${wiremock.server.baseUrl}")
   private lateinit var wireMockServerBaseUrl: String

   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @Autowired
   private lateinit var objectMapper: ObjectMapper
   private val typeRef: TypeReference<Map<String, Any?>?> = object : TypeReference<Map<String, Any?>?>() {}

   /**
    * see "authorisation/user-role-mappings.conf" in resources.
    */
   private  val adminUserName = "adminUser"
   private val platformManagerUser = "platformManager"
   private val queryRunnerUser = "queryExecutor"
   private val viewerUserName = "viewer"

   @TestConfiguration
   class TestVyneAuthorisationConfig {
      @Primary
      @Bean
      fun vyneAuthorisationConfig(): VyneAuthorisationConfig {
         val testUserRoleMappingFile = ClassPathResource("authorisation/user-role-mappings.conf").file
         return VyneAuthorisationConfig().apply {
            userToRoleMappingsFile = testUserRoleMappingFile.toPath()
         }
      }
   }

   @Test
   fun `unauthenticated user can not can not execute query`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = issueVyneQuery(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   @Test
   fun `a viewer user can not execute query`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a user without Query Runner role can not execute query`() {
      val token = setUpLoggedInUser("userWithoutAnyRoleSetup")
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a query runner can execute query`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      val responseMap = objectMapper.readValue(response.body, typeRef)
      responseMap!!["message"]!!.toString().should.equal("No strategy found for discovering type io.vyne.Username[]")
   }

   @Test
   fun `a platform manager can not execute query`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `an admin user can execute query`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = issueVyneQuery(headers)
      val responseMap = objectMapper.readValue(response.body, typeRef)
      responseMap!!["message"]!!.toString().should.equal("No strategy found for discovering type io.vyne.Username[]")
   }

   /**
    * Start get historical query list
    */
   @Test
   fun `an admin user can see query history list`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can see query history list`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not see query history list`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not see query history list`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = listQueryHistory(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not see query history list`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = listQueryHistory(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
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
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can see query result`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not see query result`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }


   @Test
   fun `a viewer user can not query result`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getQueryResult(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   /**
    * End get historical query result
    */

   /**
    * Start Get Casks
    */
   @Test
   fun `an admin user can see casks`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getCasks(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can see casks`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getCasks(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not see casks`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getCasks(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not see casks`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getCasks(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not see casks`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getCasks(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Get Casks
    */

   /**
    * Start Edit Casks
    */
   @Test
   fun `an admin user can edit casks`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteCask(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can edit casks`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteCask(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not edit casks`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteCask(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not edit casks`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteCask(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not edit casks`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = deleteCask(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Edit Casks
    */

   /**
    * Start Get Pipelines
    */
   @Test
   fun `an admin user can get pipelines`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can get pipelines`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not get pipelines`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not get pipelines`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getPipelines(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not get pipelines`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getPipelines(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Get Pipelines
    */

   /**
    * Start Delete Pipelines
    */
   @Test
   fun `an admin user can delete pipelines`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deletePipeline(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can delete pipelines`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deletePipeline(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not delete pipelines`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deletePipeline(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not delete pipelines`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deletePipeline(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not delete pipelines`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = deletePipeline(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Delete Pipelines
    */

   /**
    * Get Authentication Tokens
    */
   @Test
   fun `an admin user can get authentication tokens`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can get authentication tokens`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not can get authentication tokens`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not get authentication tokens`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getAuthenticationTokens(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not get authentication tokens`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getAuthenticationTokens(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Authentication Tokens
    */

   /**
    * Delete Authentication Token
    */
   @Test
   fun `an admin user can delete authentication tokens`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can delete authentication tokens`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not delete authentication tokens`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not delete authentication tokens`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = deleteAuthenticationToken(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not delete authentication tokens`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = deleteAuthenticationToken(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
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
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can get jdbc connections`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not get jdbc connections`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not get jdbc connections`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getJdbcConnections(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not get jdbc connections`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getJdbcConnections(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Get Jdbc Connections.
    */

   /**
    * Create jdbc connection
    */
   @Test
   fun `an admin user can create jdbc connections`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can create jdbc connections`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a query runner can not create jdbc connections`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not create jdbc connections`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = createJdbcConnection(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not create jdbc connections`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = createJdbcConnection(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Create Jdbc Connection
    */

   /**
    * Start Get user role definitions
    */
   @Test
   fun `an admin user can get vyne user role definitions`() {
      val token = setUpLoggedInUser(adminUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getUserRoleDefinitions(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_OK)
   }

   @Test
   fun `a platform manager can not get vyne user role definitions`() {
      val token = setUpLoggedInUser(platformManagerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getUserRoleDefinitions(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a query runner can not get vyne user role definitions`() {
      val token = setUpLoggedInUser(queryRunnerUser)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getUserRoleDefinitions(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `a viewer user can not get vyne user role definitions`() {
      val token = setUpLoggedInUser(viewerUserName)
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val response = getUserRoleDefinitions(headers)
      response.statusCodeValue.should.equal(HttpStatus.SC_FORBIDDEN)
   }

   @Test
   fun `unauthenticated user can not get vyne user role definitions`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val response = getUserRoleDefinitions(headers)
      response.statusCodeValue.should.be.equal(HttpStatus.SC_UNAUTHORIZED)
   }

   /**
    * End Get user role definitions
    */

   private fun setUpLoggedInUser(userName: String): String {
      val setUpIdpJwt = JWSBuilder.setUpRsaJsonWebKey(userName)
      this.jwsBuilder = setUpIdpJwt.first
      this.rsaJsonWebKey = setUpIdpJwt.second
      JWSBuilder.initialiseIdpServer(wireMockServerBaseUrl, this.jwsBuilder!!, this.rsaJsonWebKey!!)
      return jwsBuilder!!.build().compactSerialization
   }

   private fun issueVyneQuery(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity("findAll { io.vyne.Username[] }", headers)
      return restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
   }

   private fun listQueryHistory(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange("/api/query/history", HttpMethod.GET, entity, String::class.java)
   }

   private fun getQueryResult(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange("/api/query/history/summary/clientId/foo", HttpMethod.GET, entity, String::class.java)
   }

   private fun getCasks(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getCasksEndPoint, HttpMethod.GET, entity, String::class.java)
   }

   private fun deleteCask(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.deleteCasksEndPoint, HttpMethod.DELETE, entity, String::class.java)
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
         DefaultJdbcConnectionConfiguration("foo", JdbcDriver.H2, mapOf(
            Pair("catalog", "dsda"), Pair("username", "foo"), Pair("password", "bar")
         )),
         headers)
      return restTemplate.exchange(JWSBuilder.createJdbcConnection, HttpMethod.POST, entity, String::class.java)
   }

   private fun getUserRoleDefinitions(headers: HttpHeaders): ResponseEntity<String> {
      val entity = HttpEntity<Unit>(headers)
      return restTemplate.exchange(JWSBuilder.getUserRoleDefinitions, HttpMethod.GET, entity, String::class.java)

   }
}


