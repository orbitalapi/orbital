package io.vyne.queryService.security

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock
import org.jose4j.json.JsonUtil
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwk.RsaJsonWebKey
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.Instant
import java.util.UUID

data class JWSBuilder(
   var rsaJsonWebKey: RsaJsonWebKey? = null,
   var claimsIssuer: String? = null,
   var claimsSubject: String? = null) {

   fun rsaJsonWebKey(rsaJsonWebKey: RsaJsonWebKey) = apply { this.rsaJsonWebKey = rsaJsonWebKey }
   fun issuer(issuer: String) = apply { this.claimsIssuer = issuer }
   fun subject(subject: String) = apply { this.claimsSubject = subject }

   fun build(): JsonWebSignature {
      // The JWT Claims Set represents a JSON object whose members are the claims conveyed by the JWT.
      val claims = JwtClaims().apply {
         jwtId = UUID.randomUUID().toString() // unique identifier for the JWT
         issuer = claimsIssuer // identifies the principal that issued the JWT
         subject = claimsSubject // identifies the principal that is the subject of the JWT
         setAudience("https://host/api") // identifies the recipients that the JWT is intended for
         setExpirationTimeMinutesInTheFuture(10F) // identifies the expiration time on or after which the JWT MUST NOT be accepted for processing
         setIssuedAtToNow() // identifies the time at which the JWT was issued
         setClaim("azp", "example-client-id") // Authorized party - the party to which the ID Token was issued
         setClaim("scope", "openid profile email") // Scope Values
         setClaim("preferred_username", claimsSubject)
         setClaim("email", "$claimsSubject@vyne.co")
      }

      val jws = JsonWebSignature().apply {
         payload = claims.toJson()
         key = rsaJsonWebKey?.privateKey // the key to sign the JWS with
         algorithmHeaderValue = rsaJsonWebKey?.algorithm // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
         keyIdHeaderValue = rsaJsonWebKey?.keyId // a hint indicating which key was used to secure the JWS
         setHeader("typ", "JWT") // the media type of this JWS
      }

      return jws
   }

   companion object {
      val typeRef: TypeReference<Map<String, Any?>?> = object : TypeReference<Map<String, Any?>?>() {}
      const val getCasksEndPoint = "/api/casks"
      const val deleteCasksEndPoint = "/api/casks/fooCask?force=false"

      fun initialiseIdpServer(wireMockServerBaseUrl: String, jwsBuilder: JWSBuilder, rsaJsonWebKey: RsaJsonWebKey) {
         jwsBuilder!!.issuer(wireMockServerBaseUrl)
         WireMock.reset()
         WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json"))
               .willReturn(
                  WireMock.aResponse()
                     .withHeader("Content-Type", "application/json")
                     .withBody(JsonWebKeySet(rsaJsonWebKey).toJson())
               )
         )

         // mocking for getting casks
         WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo(getCasksEndPoint))
               .willReturn(
                  WireMock.aResponse()
                     .withHeader("Content-Type", "application/json")
                     .withBody("[]")
               )
         )

         // mocking for deleting a cask
         WireMock.stubFor(
            WireMock.delete(WireMock.urlEqualTo(deleteCasksEndPoint))
               .willReturn(
                  WireMock.aResponse()
                     .withHeader("Content-Type", "application/json")
                     .withBody(JsonUtil.toJson(mapOf(
                        Pair("tableName", "foo"),
                        Pair("qualifiedTypeName", "foo"),
                        Pair("versionHash", "2xas"),
                        Pair("insertedAt", Instant.now().toString())
                     ))
                     )
               )
         )

      }

       fun setUpRsaJsonWebKey(sub: String): Pair<JWSBuilder, RsaJsonWebKey> {
         val jwsBuilder = JWSBuilder().subject(sub)
         val rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
         rsaJsonWebKey.apply {
            keyId = UUID.randomUUID().toString()
            algorithm = AlgorithmIdentifiers.RSA_USING_SHA256
            use = "sig"
         }

         jwsBuilder.rsaJsonWebKey(rsaJsonWebKey)
          return Pair(jwsBuilder, rsaJsonWebKey)
      }

      fun httpHeadersWithBearerAuthorisation(token: String): HttpHeaders {
         val headers = HttpHeaders()
         headers.contentType = MediaType.APPLICATION_JSON
         headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
         headers.set("Authorization", "Bearer $token")
         return headers
      }
   }
}
