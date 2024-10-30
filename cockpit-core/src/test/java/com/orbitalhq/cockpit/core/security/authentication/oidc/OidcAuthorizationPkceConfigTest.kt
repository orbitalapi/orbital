package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.orbitalhq.auth.authorisation.UserRole
import com.orbitalhq.auth.authorisation.VyneDefaultUserRoleMappings
import com.orbitalhq.auth.authorisation.VyneUserAuthorisationRoleDefinition
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionRepository
import com.orbitalhq.cockpit.core.security.authorisation.SimplePathBasedRolesExtractor
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import com.orbitalhq.security.VyneGrantedAuthority
import com.winterbe.expekt.should
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.Date

class OidcAuthorizationPkceConfigTest {
    private val clientId = "clientId"
    private val clientSecret = "clientSecret"

    private val vyneUserRoleDefinitionRepository = object: VyneUserRoleDefinitionRepository {
        override fun findByRoleName(vyneRole: UserRole): VyneUserAuthorisationRoleDefinition = TODO("Not yet implemented")
        override fun findAll(): Map<UserRole, VyneUserAuthorisationRoleDefinition> {
           return mapOf("Admin" to VyneUserAuthorisationRoleDefinition(setOf(VyneGrantedAuthority.RunQuery)))
        }
        override fun defaultUserRoles(): VyneDefaultUserRoleMappings =  TODO("Not yet implemented")
        override fun defaultApiClientUserRoles(): VyneDefaultUserRoleMappings =  TODO("Not yet implemented")
    }

    private lateinit var mockWebServer: MockWebServer

    private val authenticationManager =
        ReactiveAuthenticationManager { authentication ->
            val token = (authentication as BearerTokenAuthenticationToken).token
            NimbusReactiveJwtDecoder(object: Converter<JWT, Mono<JWTClaimsSet>>  {
                override fun convert(source: JWT): Mono<JWTClaimsSet>? {
                    val claimSet = JWTClaimsSet
                        .Builder()
                        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                        .issuer(mockWebServer.url("/").toUrl().toString())
                        .subject("application-client")
                        .audience("application-client")
                        .issueTime(Date())
                        .claim("roles", listOf("Admin"))
                        .notBeforeTime(Date())
                        .jwtID("00844099-8108-4be0-a397-51baf30cb5f5")
                        .claim("preferred_username", "application-client")
                        .build()
                    return Mono.just(claimSet)
                }

            }).decode(token).map { jwt ->
                val jwtAuthenticationConverter = JwtAuthenticationConverter()
                val roleExtractor = SimplePathBasedRolesExtractor("roles")
                val grantedAuthoritiesExtractor =GrantedAuthoritiesExtractor (vyneUserRoleDefinitionRepository, roleExtractor)
                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesExtractor)
                jwtAuthenticationConverter.convert(jwt)
            }
        }

    @BeforeEach
    fun setupMockOpenIdpServer() {
        mockWebServer = MockWebServer()
    }

    @Test
    fun `When executorRoleTokenUrl is provided then token_endpoint url not discovered from OpenID provider metadata`() {
        val vyneOpenIdpConnectConfig = VyneOpenIdpConnectConfig(
            oidcDiscoveryUrl = null,
            executorRoleTokenUrl = mockWebServer.url("/").toUrl().toString(),
            executorRoleClientId = clientId,
            executorRoleClientSecret = clientSecret)

       val oAuthExecutionPrincipalAuthService =
           OidcAuthorizationPkceConfig().executionPrincipalAuthService(vyneOpenIdpConnectConfig, authenticationManager, WebClient.builder())


        enqueueTokenResponse()

        StepVerifier.create(oAuthExecutionPrincipalAuthService!!.loadUser())
            .expectSubscription()
            .expectNextMatches { vyneUser ->
                val tokenRequest = mockWebServer.takeRequest()
                val requestBody = tokenRequest.body.readUtf8()
                val rolesInClaims = vyneUser.claims["roles"] as List<String>
                rolesInClaims.contains("Admin") &&
                        requestBody == "grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret"
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `When executorRoleTokenUrl is not provided then token_endpoint url is discovered from OpenID provider metadata`() {
        val vyneOpenIdpConnectConfig = VyneOpenIdpConnectConfig(
            oidcDiscoveryUrl = mockWebServer.url("/").toUrl().toString(),
            executorRoleTokenUrl = null,
            executorRoleClientId = clientId,
            executorRoleClientSecret = clientSecret)

        // SetUp Mock OpenIdp to provide response for .well-known/openid-configuration
        enqueueWellKnownOpenIdpConfiguration()

        // This will fetch token_endpoint from http://<IDP_SERVER>/.well-known/openid-configuration
        val oAuthExecutionPrincipalAuthService =
            OidcAuthorizationPkceConfig().executionPrincipalAuthService(vyneOpenIdpConnectConfig, authenticationManager, WebClient.builder())

        mockWebServer.takeRequest().requestUrl.should.equal(mockWebServer.url("/.well-known/openid-configuration "))

        enqueueTokenResponse()

        StepVerifier.create(oAuthExecutionPrincipalAuthService!!.loadUser())
            .expectSubscription()
            .expectNextMatches { vyneUser ->
                val tokenRequest = mockWebServer.takeRequest()
                val requestBody = tokenRequest.body.readUtf8()
                val rolesInClaims = vyneUser.claims["roles"] as List<String>
                rolesInClaims.contains("Admin") &&
                        requestBody == "grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret"
            }
            .thenCancel()
            .verify()

    }

    private fun enqueueTokenResponse() {
        mockWebServer.enqueue(
             MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                    {
                        "access_token": "eyJraWQiOiIxYzRkY2M0ZC1kYWU3LTRmMzktYTNjMS1kNWRlMmU2MDEyMjMiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhcHBsaWNhdGlvbi1jbGllbnQiLCJhdWQiOiJhcHBsaWNhdGlvbi1jbGllbnQiLCJuYmYiOjE3MzAyNzI4MDgsImNsaWVudElkIjoiZmxvdy1jbGllbnQiLCJyb2xlcyI6WyJBZG1pbiJdLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwMDAiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiIsImV4cCI6MTczMDI3NjQwOCwiaWF0IjoxNzMwMjcyODA4LCJqdGkiOiI3MjY0M2JkMy0wYjUxLTRlNWItYmNkMy01MTMzMjdmYWRhOWMifQ.VaOJnsH4stcmze0Aq6DwxYiy859eEcc_gVgxl87pPYz5MkPnY3QDw0ASABVMZOBh-fpGouP7sciw0YteE9AfTtQdh-rtjFE2lYvy0ukWEurhyZ-GyTyTO9SWA9lEHqzcP9E8XMrQHevDHbHB30RVq1NDcxpH7K_OT7nN91-93xO0f-0szv8_gFutWyQ7s_gKtK_Lvb_cQRIAudb-vx6ubd_S-a7fr859a7Nnhs-nrHMai41eOvt57Mnmu9ACxXtqJOS6A1SwZ0ZE9f-zGfalw4C7h5bVHPxRIdgJGvNgQOd6fBBJqVObwoHUJe15RZqHFUr_j8ilsiM5iNqGXZrCWg",
                        "token_type": "Bearer",
                        "expires_in": 3599
                    }
                """.trimIndent())
        )

    }

    private fun enqueueWellKnownOpenIdpConfiguration() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                    {
  "issuer": "${mockWebServer.url("/").toUrl()}",
  "authorization_endpoint": "http://localhost:9000/oauth2/authorize",
  "device_authorization_endpoint": "http://localhost:9000/oauth2/device_authorization",
  "token_endpoint": "${mockWebServer.url("/oauth2/token").toUrl()}",
  "token_endpoint_auth_methods_supported": [
    "client_secret_basic",
    "client_secret_post",
    "client_secret_jwt",
    "private_key_jwt",
    "tls_client_auth",
    "self_signed_tls_client_auth"
  ],
  "jwks_uri": "http://localhost:9000/oauth2/jwks",
  "userinfo_endpoint": "http://localhost:9000/userinfo",
  "end_session_endpoint": "http://localhost:9000/connect/logout",
  "response_types_supported": [
    "code"
  ],
  "grant_types_supported": [
    "authorization_code",
    "client_credentials",
    "refresh_token",
    "urn:ietf:params:oauth:grant-type:device_code",
    "urn:ietf:params:oauth:grant-type:token-exchange"
  ],
  "revocation_endpoint": "http://localhost:9000/oauth2/revoke",
  "revocation_endpoint_auth_methods_supported": [
    "client_secret_basic",
    "client_secret_post",
    "client_secret_jwt",
    "private_key_jwt",
    "tls_client_auth",
    "self_signed_tls_client_auth"
  ],
  "introspection_endpoint": "http://localhost:9000/oauth2/introspect",
  "introspection_endpoint_auth_methods_supported": [
    "client_secret_basic",
    "client_secret_post",
    "client_secret_jwt",
    "private_key_jwt",
    "tls_client_auth",
    "self_signed_tls_client_auth"
  ],
  "code_challenge_methods_supported": [
    "S256"
  ],
  "tls_client_certificate_bound_access_tokens": true,
  "subject_types_supported": [
    "public"
  ],
  "id_token_signing_alg_values_supported": [
    "RS256"
  ],
  "scopes_supported": [
    "openid"
  ]
}
                """.trimIndent())
        )
    }

}