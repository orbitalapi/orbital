package io.vyne.queryService.security.authorisation.rest

import io.vyne.queryService.security.authorisation.VyneAuthorisationConfig
import io.vyne.spring.config.TestDiscoveryClientConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import java.io.File


@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}",
      "spring.datasource.url=jdbc:h2:mem:UserRoleMappingControllerTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY",
   ])
class UserRoleMappingControllerTest {
   @Autowired
   private lateinit var webClient: WebTestClient

   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class TestVyneAuthorisationConfig {
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
   fun `fetch user role definitions`() {
      StepVerifier.create(
         webClient
         .get().uri("/api/user/roles")
         .exchange()
         .returnResult(Any::class.java)
         .responseBody
      ).expectSubscription()
         .expectNext("Admin")
         .expectNext("PlatformManager")
         .expectNext("QueryRunner")
         .expectNext("Viewer")
         .verifyComplete()
   }

   @Test
   fun `save and fetch roles for user`() {
      val username = "test1"
      // test1 has not roles assigned.
      webClient
         .get().uri("/api/user/roles/$username")
         .exchange()
         .expectStatus()
         .isOk
         .expectBody()
         .json("[]")

      // assign roles to test1
      StepVerifier.create(
      webClient
         .post().uri("/api/user/roles/$username")
         .bodyValue(
            setOf("QueryRunner", "PlatformManager"))
         .exchange()
         .returnResult(Any::class.java)
         .responseBody
      )
         .expectSubscription()
         .expectNext("PlatformManager")
         .expectNext("QueryRunner")
         .verifyComplete()

      // re-fetch test1 roles
      StepVerifier.create(
      webClient
         .get().uri("/api/user/roles/$username")
         .exchange()
         .returnResult(Any::class.java)
         .responseBody
      )
         .expectSubscription()
         .expectNext("PlatformManager")
         .expectNext("QueryRunner")
         .verifyComplete()
   }
}
