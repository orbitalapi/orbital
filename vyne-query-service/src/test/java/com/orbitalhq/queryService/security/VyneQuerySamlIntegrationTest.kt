package com.orbitalhq.queryService.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.gargoylesoftware.htmlunit.html.HtmlButton
import com.gargoylesoftware.htmlunit.html.HtmlHeading1
import com.gargoylesoftware.htmlunit.html.HtmlInput
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.security.authorisation.VyneAuthorisationConfig
import com.orbitalhq.cockpit.core.security.authorisation.VyneSamlConfig
import com.orbitalhq.copilot.OpenAiChatService
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.queryService.TestSchemaProvider
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
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.TestSocketUtils
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.netty.http.server.HttpServer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString


@ExtendWith(SpringExtension::class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}",
      "logging.level.org.springframework.security=DEBUG",
      "logging.level.org.pac4j.core.engine=DEBUG",
      "pac4j.callback.path=/saml",
      "vyne.analytics.persistResults=true",
      "vyne.telemetry.enabled=false"
   ]
)
@Testcontainers
class VyneQuerySamlIntegrationTest {
   companion object {
      private const val orbitalSpId = "http://foo.orbitalhq.io"

      @MockBean
      lateinit var chatService: OpenAiChatService

      @JvmStatic
      @TempDir
      private lateinit var tempDir: Path
      private lateinit var idpMetadataXmlPath: String
      @Container
      private val samlPhpContainer = SimpleSamlPhpContainer()
         .withExposedPorts(8080)
         .withEnv("SIMPLESAMLPHP_SP_ENTITY_ID", orbitalSpId)
         // We'll override this below as our port number is not fixed (not 9022)
        // .withEnv("SIMPLESAMLPHP_SP_ASSERTION_CONSUMER_SERVICE", "http://localhost:9022/saml?client_name=SAML2Client")


      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

      @JvmStatic
      @DynamicPropertySource
      fun properties(registry: DynamicPropertyRegistry) {
         val metadata = "\$metadata"

         // Patching saml20-idp-hosted.php  in SimplePhpSaml as by default, urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST is not enabled for single sign-on.
         val saml20_idp_hosted_php = """
            <?php

            /**
             * SAML 2.0 IdP configuration for SimpleSAMLphp.
             *
             * See: https://simplesamlphp.org/docs/stable/simplesamlphp-reference-idp-hosted
             */

            $metadata['__DYNAMIC:1__'] = [
                'SingleSignOnServiceBinding' => array('urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST'),
                'SingleLogoutServiceBinding' => array('urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST'),
                /*
                 * The hostname of the server (VHOST) that will use this SAML entity.
                 *
                 * Can be '__DEFAULT__', to use this entry by default.
                 */
                'host' => '__DEFAULT__',

                // X.509 key and certificate. Relative to the cert directory.
                'privatekey' => 'server.pem',
                'certificate' => 'server.crt',

                /*
                 * Authentication source to use. Must be one that is configured in
                 * 'config/authsources.php'.
                 */
                'auth' => 'example-userpass',

                /* Uncomment the following to use the uri NameFormat on attributes. */
                /*
                'attributes.NameFormat' => 'urn:oasis:names:tc:SAML:2.0:attrname-format:uri',
                'authproc' => [
                    // Convert LDAP names to oids.
                    100 => ['class' => 'core:AttributeMap', 'name2oid'],
                ],
                */

                /*
                 * Uncomment the following to specify the registration information in the
                 * exported metadata. Refer to:
                 * http://docs.oasis-open.org/security/saml/Post2.0/saml-metadata-rpi/v1.0/cs01/saml-metadata-rpi-v1.0-cs01.html
                 * for more information.
                 */
                /*
                'RegistrationInfo' => [
                    'authority' => 'urn:mace:example.org',
                    'instant' => '2008-01-17T11:28:03Z',
                    'policies' => [
                        'en' => 'http://example.org/policy',
                        'es' => 'http://example.org/politica',
                    ],
                ],
                */
            ];
         """.trimIndent()
         samlPhpContainer
            .withCopyToContainer(Transferable.of(saml20_idp_hosted_php), "/var/www/simplesamlphp/metadata/saml20-idp-hosted.php")
            .start()
         samlPhpContainer.waitingFor(Wait.forListeningPort())
         // Configuration Coming from Saml Idp.
         val samlIdpMetadata =  """
            <?xml version="1.0"?>
            <md:EntitiesDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
               <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="http://${samlPhpContainer.host}:${samlPhpContainer.firstMappedPort}/simplesaml/saml2/idp/metadata.php">
                 <md:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
                   <md:KeyDescriptor use="signing">
                     <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                       <ds:X509Data>
                         <ds:X509Certificate>MIICmjCCAYICCQDX5sKPsYV3+jANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTE5MTIyMzA5MDI1MVoXDTIwMDEyMjA5MDI1MVowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMdtDJ278DQTp84O5Nq5F8s5YOR34GFOGI2Swb/3pU7X7918lVljiKv7WVM65S59nJSyXV+fa15qoXLfsdRnq3yw0hTSTs2YDX+jl98kK3ksk3rROfYh1LIgByj4/4NeNpExgeB6rQk5Ay7YS+ARmMzEjXa0favHxu5BOdB2y6WvRQyjPS2lirT/PKWBZc04QZepsZ56+W7bd557tdedcYdY/nKI1qmSQClG2qgslzgqFOv1KCOw43a3mcK/TiiD8IXyLMJNC6OFW3xTL/BG6SOZ3dQ9rjQOBga+6GIaQsDjC4Xp7Kx+FkSvgaw0sJV8gt1mlZy+27Sza6d+hHD2pWECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAm2fk1+gd08FQxK7TL04O8EK1f0bzaGGUxWzlh98a3Dm8+OPhVQRi/KLsFHliLC86lsZQKunYdDB+qd0KUk2oqDG6tstG/htmRYD/S/jNmt8gyPAVi11dHUqW3IvQgJLwxZtoAv6PNs188hvT1WK3VWJ4YgFKYi5XQYnR5sv69Vsr91lYAxyrIlMKahjSW1jTD3ByRfAQghsSLk6fV0OyJHyhuF1TxOVBVf8XOdaqfmvD90JGIPGtfMLPUX4m35qaGAU48PwCL7L3cRHYs9wZWc0ifXZcBENLtHYCLi5txR8c5lyHB9d3AQHzKHMFNjLswn5HsckKg83RH7+eVqHqGw==</ds:X509Certificate>
                       </ds:X509Data>
                     </ds:KeyInfo>
                   </md:KeyDescriptor>
                   <md:KeyDescriptor use="encryption">
                     <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                       <ds:X509Data>
                         <ds:X509Certificate>MIICmjCCAYICCQDX5sKPsYV3+jANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTE5MTIyMzA5MDI1MVoXDTIwMDEyMjA5MDI1MVowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMdtDJ278DQTp84O5Nq5F8s5YOR34GFOGI2Swb/3pU7X7918lVljiKv7WVM65S59nJSyXV+fa15qoXLfsdRnq3yw0hTSTs2YDX+jl98kK3ksk3rROfYh1LIgByj4/4NeNpExgeB6rQk5Ay7YS+ARmMzEjXa0favHxu5BOdB2y6WvRQyjPS2lirT/PKWBZc04QZepsZ56+W7bd557tdedcYdY/nKI1qmSQClG2qgslzgqFOv1KCOw43a3mcK/TiiD8IXyLMJNC6OFW3xTL/BG6SOZ3dQ9rjQOBga+6GIaQsDjC4Xp7Kx+FkSvgaw0sJV8gt1mlZy+27Sza6d+hHD2pWECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAm2fk1+gd08FQxK7TL04O8EK1f0bzaGGUxWzlh98a3Dm8+OPhVQRi/KLsFHliLC86lsZQKunYdDB+qd0KUk2oqDG6tstG/htmRYD/S/jNmt8gyPAVi11dHUqW3IvQgJLwxZtoAv6PNs188hvT1WK3VWJ4YgFKYi5XQYnR5sv69Vsr91lYAxyrIlMKahjSW1jTD3ByRfAQghsSLk6fV0OyJHyhuF1TxOVBVf8XOdaqfmvD90JGIPGtfMLPUX4m35qaGAU48PwCL7L3cRHYs9wZWc0ifXZcBENLtHYCLi5txR8c5lyHB9d3AQHzKHMFNjLswn5HsckKg83RH7+eVqHqGw==</ds:X509Certificate>
                       </ds:X509Data>
                     </ds:KeyInfo>
                   </md:KeyDescriptor>
                   <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://${samlPhpContainer.host}:${samlPhpContainer.firstMappedPort}/simplesaml/saml2/idp/SingleLogoutService.php"/>
                   <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="http://${samlPhpContainer.host}:${samlPhpContainer.firstMappedPort}/simplesaml/saml2/idp/SingleLogoutService.php"/>
                   <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>
                   <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://${samlPhpContainer.host}:${samlPhpContainer.firstMappedPort}/simplesaml/saml2/idp/SSOService.php"/>
                   <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="http://${samlPhpContainer.host}:${samlPhpContainer.firstMappedPort}/simplesaml/saml2/idp/SSOService.php"/>
                 </md:IDPSSODescriptor>
                 <md:ContactPerson contactType="technical">
                   <md:GivenName>Administrator</md:GivenName>
                   <md:EmailAddress>mailto:na@example.com</md:EmailAddress>
                 </md:ContactPerson>
               </md:EntityDescriptor>
            </md:EntitiesDescriptor>
         """.trimIndent()

         val tempFilePath =  tempDir.resolve("simple-saml-metadata.xml")
         Files.write(tempDir.resolve("simple-saml-metadata.xml"), samlIdpMetadata.toByteArray(Charsets.UTF_8))
         idpMetadataXmlPath = tempFilePath.absolutePathString()

         registry.add("vyne.security.saml.enabled") { "true"}
         registry.add("vyne.security.saml.idpMetadataFilePath") { idpMetadataXmlPath }
         registry.add("vyne.security.saml.serviceProviderEntityId") { orbitalSpId }
         registry.add("vyne.security.saml.keyStorePassword") { "orbital" }
         registry.add("vyne.security.saml.privateKeyPassword") { "orbital" }
      }
   }

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

   @MockBean
   lateinit var packagesService: PackageService

   @MockBean
   lateinit var schemaEditorService: SchemaEditorService

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

   @Autowired
   lateinit var randomServerPort: RandomServerPort


   @TestConfiguration
   @Import(TestDiscoveryClientConfig::class)
   class TestVyneAuthorisationConfig {


      @Bean
      fun randomServerPort(): RandomServerPort {
         return RandomServerPort(TestSocketUtils.findAvailableTcpPort())
      }

      @Bean
      fun vyneSamlConfig(randomServerPort: RandomServerPort) : VyneSamlConfig {
         // We re-inject VyneSamlConfig as our server port is known at this point.
         return VyneSamlConfig(
            enabled = true,
            keyStorePath = tempDir.resolve("orbital-saml.jks").absolutePathString(),
            keyStorePassword = "orbital",
            privateKeyPassword = "orbital",
            idpMetadataFilePath = idpMetadataXmlPath,
           serviceProviderMetadataResourcePath = tempDir.resolve("sp-metadata.xml").absolutePathString(),
           callbackBaseUrl= "http://localhost:${randomServerPort.serverPort}",
           serviceProviderEntityId = orbitalSpId,
           maximumAuthenticationLifetime = 3600
         )
      }

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

      @Primary
      @Bean
      fun nettyReactiveWebServerFactory(randomServerPort: RandomServerPort): NettyReactiveWebServerFactory? {
         val webServerFactory = NettyReactiveWebServerFactory()
         webServerFactory.addServerCustomizers(object: NettyServerCustomizer {
            override fun apply(httpServer: HttpServer): HttpServer {
              return httpServer.port(randomServerPort.serverPort)
            }
         }
         )
         return webServerFactory
      }
   }

   @Test
   fun `can login through saml`() {
      val metadata = "\$metadata"

      // Another SamlSimplePhp workaround, we need register orbital as an AssertionConsumerService
      val saml20_sp_remote_php = """
         <?php
         /**
          * SAML 2.0 remote SP metadata for SimpleSAMLphp.
          *
          * See: https://simplesamlphp.org/docs/stable/simplesamlphp-reference-sp-remote
          */

         if (!getenv('SIMPLESAMLPHP_SP_ENTITY_ID')) {
             throw new UnexpectedValueException('SIMPLESAMLPHP_SP_ENTITY_ID is not defined as an environment variable.');
         }

         $metadata[getenv('SIMPLESAMLPHP_SP_ENTITY_ID')] = array(
             'AssertionConsumerService' => 'http://localhost:${randomServerPort.serverPort}/saml?client_name=SAML2Client',
             'SingleLogoutService' => getenv('SIMPLESAMLPHP_SP_SINGLE_LOGOUT_SERVICE'),
         );

      """.trimIndent()

      samlPhpContainer
         .copyFileToContainer(Transferable.of(saml20_sp_remote_php), "/var/www/simplesamlphp/metadata/saml20-sp-remote.php")
      com.gargoylesoftware.htmlunit.WebClient().use { webClient ->
         // now you have a running browser, and you can start doing real things
         // like going to a web page
         webClient.options.isJavaScriptEnabled = true


         // Once we authenticated Saml Idp will redirect to -> http://localhost:port/index.html
         // which will end up with '404' as we don't have the frontend code in the test
         // so prevent htmlunit to throw for 404
         webClient.options.isThrowExceptionOnFailingStatusCode = false

         // Visit Orbital
         val page: HtmlPage = webClient.getPage("http://localhost:${randomServerPort.serverPort}")
         // We should be redirected to Saml Login page on Idp
         page.titleText.should.equal("Enter your username and password")
         val htmlForm = page.forms.first()
         val username = htmlForm.getInputByName<HtmlInput>("username")
         val password = htmlForm.getInputByName<HtmlInput>("password")
         val button = page.getFirstByXPath<HtmlButton>("//button[@type='submit']")
         username.type("user1")
         webClient.waitForBackgroundJavaScriptStartingBefore(10000L)
         password.type("password")
         webClient.waitForBackgroundJavaScriptStartingBefore(10000L)
         // Do Login
         val resultPage = button.click<HtmlPage>()
         webClient.waitForBackgroundJavaScriptStartingBefore(10000L)
         // We should be redirected back to Orbital but since we don't have the frontend code
         // we display the whitelabel error page.
         resultPage
            .getFirstByXPath<HtmlHeading1>("//h1")
            .textContent
            .should
            .equal("Whitelabel Error Page")
      }
   }
}

class SimpleSamlPhpContainer: GenericContainer<SimpleSamlPhpContainer>("kenchan0130/simplesamlphp")
data class RandomServerPort(val serverPort: Int)
