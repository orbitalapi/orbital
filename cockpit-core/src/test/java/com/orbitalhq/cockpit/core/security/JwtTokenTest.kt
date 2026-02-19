package com.orbitalhq.cockpit.core.security

import com.orbitalhq.auth.authentication.toVyneUser
import com.orbitalhq.cockpit.core.security.authorisation.SimplePathBasedRolesExtractor
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSize
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

class JwtTokenTest {
    @Test
    fun `can process a client credentials token`() {
        val jwt = jwt()
        val jwtAuthenticationToken =   JwtAuthenticationToken(jwt)
        val vyneUser = jwtAuthenticationToken.toVyneUser()
        vyneUser.username.should.equal("4f76ac09-a587-48ec-a22a-43060003ba2f")
        // No roles element in the token
        SimplePathBasedRolesExtractor("roles").getRoles(jwt).shouldHaveSize(0)
    }

    private fun jwt(): Jwt {
        val rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
        rsaJsonWebKey.apply {
            keyId = UUID.randomUUID().toString()
            algorithm = AlgorithmIdentifiers.RSA_USING_SHA256
            use = "sig"
        }
        val claims = JwtClaims().apply {
            jwtId = UUID.randomUUID().toString() // unique identifier for the JWT
            issuer = "https://login.microsoftonline.com/0592515b-94ca-42b2-ad9b-65bfb2104867/v2.0" // identifies the principal that issued the JWT
            subject = "4f76ac09-a587-48ec-a22a-43060003ba2f" // identifies the principal that is the subject of the JWT
            setAudience("https://host/api") // identifies the recipients that the JWT is intended for
            setExpirationTimeMinutesInTheFuture(10F) // identifies the expiration time on or after which the JWT MUST NOT be accepted for processing
            setIssuedAtToNow() // identifies the time at which the JWT was issued
            setClaim("azp", "example-client-id") // Authorized party - the party to which the ID Token was issued
            setClaim("scope", "openid profile email") // Scope Values

        }

        val clientCredentialsBearerToken = JsonWebSignature().apply {
            payload = claims.toJson()
            key = rsaJsonWebKey.privateKey // the key to sign the JWS with
            algorithmHeaderValue = rsaJsonWebKey.algorithm // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
            keyIdHeaderValue = rsaJsonWebKey.keyId // a hint indicating which key was used to secure the JWS
            setHeader("typ", "JWT") // the media type of this JWS
        }.compactSerialization


        return NimbusReactiveJwtDecoder.withPublicKey(rsaJsonWebKey.getRsaPublicKey()).build().decode(clientCredentialsBearerToken).block()!!



    }
}
