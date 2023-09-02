package com.orbitalhq.queryService.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.booleans.shouldBeTrue
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.queryService.TestSchemaProvider
import com.orbitalhq.queryService.VyneQueryIntegrationTest
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
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
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

private val logger = KotlinLogging.logger {  }
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
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=\${wiremock.server.baseUrl}/.well-known/jwks.json",
      "vyne.security.openIdp.enabled=true",
      "vyne.telemetry.enabled=false",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
   ])
class VyneQuerySecurityFirstTimeLoginIntegrationTest {
   private var rsaJsonWebKey: RsaJsonWebKey? = null
   private var jwsBuilder: JWSBuilder? = null

   @Value("\${wiremock.server.baseUrl}")
   private lateinit var wireMockServerBaseUrl: String

   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @Autowired
   private lateinit var objectMapper: ObjectMapper

   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class TestVyneAuthorisationConfig {
      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider =
         TestSchemaProvider.withBuiltInsAnd(VyneQueryIntegrationTest.UserSchema.schema)

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
   fun `when the first user logs in it is set as admin`() {
      val setUpIdpJwt = JWSBuilder.setUpRsaJsonWebKey("firstUser")
      this.jwsBuilder = setUpIdpJwt.first
      this.rsaJsonWebKey = setUpIdpJwt.second
      JWSBuilder.initialiseIdpServer(wireMockServerBaseUrl, this.jwsBuilder!!, this.rsaJsonWebKey!!)
      val token = jwsBuilder!!.build().compactSerialization
      val headers = JWSBuilder.httpHeadersWithBearerAuthorisation(token)
      val entity = HttpEntity("find { com.orbitalhq.Username[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
      logger.info { "$response" }
      response.statusCode.is2xxSuccessful.shouldBeTrue()
   }
}
