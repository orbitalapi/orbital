package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.auth.authentication.JwtStandardClaims
import com.orbitalhq.cockpit.core.security.authentication.oidc.propelAuth.PropelAuthApiKeyValidationResponse
import com.orbitalhq.cockpit.core.security.authorisation.propelauth.CloudPropelAuthClaimsExtractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.StandardMongoClientSettingsBuilderCustomizer
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant

@ConditionalOnProperty("vyne.security.openIdp.roles.format", havingValue = CloudPropelAuthClaimsExtractor.PropelAuthJwtKind, matchIfMissing = false)
@Component
class PropelAuthApiKeyValidator(
   private val restClient: RestClient.Builder = RestClient.builder(),
   @Value("\${vyne.security.openIdp.issuerUrl}")
   private val baseUrl: String,
   @Value("\${vyne.security.propelAuth.apiKey}")
   private val propelAuthApiKey: String,
   private val grantedAuthoritiesExtractor: GrantedAuthoritiesExtractor,
) : ApiKeyValidator {
   private val webClient = restClient.baseUrl(baseUrl)
      .build()
   private val mapper = objectMapper()
   private val jwtAuthConverter = JwtAuthenticationConverter().apply {
      setJwtGrantedAuthoritiesConverter(grantedAuthoritiesExtractor)
   }

   companion object {
      fun objectMapper(): ObjectMapper {
         return jacksonObjectMapper()
            .disable(
               DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
               DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
            )
      }
   }
   override fun authenticate(authentication: Authentication): Mono<Authentication> {
      return Mono.create { sink ->
         val response =  webClient.post().uri("/api/backend/v1/end_user_api_keys/validate")
            .header("Authorization", "Bearer $propelAuthApiKey")
            .header("Content-Type","application/json")
            .body(ApiTokenValidationRequest(authentication.principal.toString()))
            .retrieve()
            .toEntity<String>()

         if (!response.statusCode.is2xxSuccessful) {
            throw BadCredentialsException("The provided token is not valid")
         }
         val responseJson = response.body ?: throw BadCredentialsException("The authentication response was not valid")
         val responsePayload = mapper.readValue<PropelAuthApiKeyValidationResponse>(responseJson)

         // Design choice: Convert the response from PropelAuth into a JWT
         // All our stack is built on the assumption of JWT for authentication.
         // WE don't have one here, but don't want that to bleed into the rest of the stack
         // so build a fake JWT.
         val userClaims = responsePayload.user?.toMutableMap() ?: throw BadCredentialsException("The user claims was not valid")
         userClaims[JwtClaimNames.SUB] = userClaims["user_id"]!!
         userClaims[JwtStandardClaims.Issuer] = baseUrl
         val jwt = Jwt(
            responseJson,
            Instant.now(),
            Instant.now().plusSeconds(600),
            mapOf(
               JwtClaimNames.ISS to baseUrl
            ),
            userClaims
         )
         val convertedToken = jwtAuthConverter.convert(jwt)
         sink.success(convertedToken)
//         sink.success(JwtAuthenticationToken(
//            jwt
//         ))
      }.subscribeOn(Schedulers.boundedElastic()) as Mono<Authentication>


   }
}

private data class ApiTokenValidationRequest(
   val api_key_token: String
)
