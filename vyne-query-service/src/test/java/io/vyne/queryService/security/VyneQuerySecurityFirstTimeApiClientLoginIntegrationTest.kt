package io.vyne.queryService.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.winterbe.expekt.should
import io.vyne.queryService.VyneQueryIntegrationTest
import io.vyne.queryService.security.authorisation.VyneAuthorisationConfig
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import mu.KotlinLogging
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

private val logger = KotlinLogging.logger {  }
@RunWith(SpringRunner::class)
@AutoConfigureWireMock(port = 0)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}",
      "spring.datasource.url=jdbc:h2:mem:testdbVyneQuerySecureIntegrationTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=\${wiremock.server.baseUrl}/.well-known/jwks.json",
      "vyne.security.openIdp.enabled=true",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
   ])
@ActiveProfiles("test")
class VyneQuerySecurityFirstTimeApiClientLoginIntegrationTest {
   private var rsaJsonWebKey: RsaJsonWebKey? = null
   private var jwsBuilder: JWSBuilder? = null

   @Value("\${wiremock.server.baseUrl}")
   private lateinit var wireMockServerBaseUrl: String

   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @Autowired
   private lateinit var objectMapper: ObjectMapper

   @TestConfiguration
   class TestVyneAuthorisationConfig {
      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider = SimpleTaxiSchemaProvider(VyneQueryIntegrationTest.UserSchema.source)

      @Bean
      fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()

      @Primary
      @Bean
      fun vyneAuthorisationConfig(): VyneAuthorisationConfig {
         return VyneAuthorisationConfig().apply {
            val tempUserToRoleMappingFile = File.createTempFile("user-role-mapping", ".conf")
            tempUserToRoleMappingFile.deleteOnExit()
            userToRoleMappingsFile = tempUserToRoleMappingFile.toPath()
         }
      }
   }

   @Test
   fun `when a api client talks to Vyne for the first time it is given QueryRunner role`() {
      val setUpIdpJwt = JWSBuilder.setUpRsaJsonWebKey("firstUser")
      setUpIdpJwt.first.clientId("api-client")
      this.jwsBuilder = setUpIdpJwt.first
      this.rsaJsonWebKey = setUpIdpJwt.second
      JWSBuilder.initialiseIdpServer(wireMockServerBaseUrl, this.jwsBuilder!!, this.rsaJsonWebKey!!)
      val token = jwsBuilder!!.build().compactSerialization
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val entity = HttpEntity("findAll { io.vyne.Username[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
      logger.info { "$response" }
      val responseMap = objectMapper.readValue(response.body, JWSBuilder.typeRef)
      responseMap!!["message"]!!.toString().should.equal("No strategy found for discovering type io.vyne.Username[]")
   }
}
