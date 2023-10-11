package com.orbitalhq.auth.schemes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.typesafe.config.Config
import com.typesafe.config.ConfigObject
import io.github.config4k.extract
import com.orbitalhq.auth.tokens.AuthToken
import com.orbitalhq.config.getSafeConfigString
import com.orbitalhq.schemas.ServiceName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.encodeToConfig
import mu.KotlinLogging

@Serializable
data class AuthTokens(
   val authenticationTokens: Map<ServiceName, AuthScheme>
) {
   companion object {
      private val logger = KotlinLogging.logger {}

      fun empty() = AuthTokens(emptyMap())

      /**
       * Workaround to https://github.com/Kotlin/kotlinx.serialization/issues/1581
       * HOCON serialization doesn't support polymorphism.
       * so we have to do it by hand.
       *
       * Currently, still using kotlinx HOCON serialziation, as it
       * outputs the correct type, though this is a pretty flimsy reason,
       * and could swap this back to some other mechanism and stick with
       * Config4k
       *
       * Otherwise, we'd call Hocon.decodeFromConfig<AuthTokens>(config)
       */
      fun fromConfig(config: Config): AuthTokens {
         if (!config.hasPath(AuthTokens::authenticationTokens.name)) {
            return empty()
         }
         val tokenConfigs = config.getObject(AuthTokens::authenticationTokens.name)
         val schemes = tokenConfigs.map { (serviceName, tokenConfig) ->
            require(tokenConfig is ConfigObject) { "Encoding error - expected a ConfigObject but was ${tokenConfig::class.simpleName}" }
            val schemeType = tokenConfig.get("type")?.unwrapped() as String?
            val authScheme = if (schemeType == null) {
               logger.error { "Configured Auth for service $serviceName does not define a type property - it looks like this is using an old format" }
               val authScheme = tokenConfig.toConfig().extract<AuthToken>().upgradeToAuthScheme()
               val newMapConfig = mapOf(serviceName to authScheme)
               val updatedConfig = authScheme.asHocon()
               logger.info { "Consider upgrading using the following config: \n${updatedConfig.getSafeConfigString()}" }
               authScheme
            } else {
               when (val schemeType = schemeType) {
                  "Basic" -> tokenConfig.toConfig().extract<BasicAuth>()
                  "HttpHeader" -> tokenConfig.toConfig().extract<HttpHeader>()
                  "QueryParam" -> tokenConfig.toConfig().extract<QueryParam>()
                  "OAuth2" -> tokenConfig.toConfig().extract<OAuth2>()
                  "Cookie" -> tokenConfig.toConfig().extract<Cookie>()
                  else -> error("Unrecognized type of auth scheme: $schemeType")
               }
            }

            serviceName to authScheme
         }.toMap()
         return AuthTokens(schemes)
      }
   }
}

typealias SanitizedAuthScheme = AuthScheme

// We're using a mix of Kotlin serialization in some places,
// And Jackson in others (primarily when sending to the UI).  Make sure the Jackson subtype
// stuff here matches the kotlin impl., otherwise stuff will break
@Serializable
@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.PROPERTY,
   property = "type"
)
@JsonSubTypes(
   JsonSubTypes.Type(BasicAuth::class, name = "Basic"),
   JsonSubTypes.Type(HttpHeader::class, name = "HttpHeader"),
   JsonSubTypes.Type(QueryParam::class, name = "QueryParam"),
   JsonSubTypes.Type(Cookie::class, name = "Cookie"),
   JsonSubTypes.Type(OAuth2::class, name = "OAuth2"),
)
sealed class AuthScheme(
) {
   abstract fun sanitized(): SanitizedAuthScheme

   companion object {
      val MASKED_PASSWORD = "**************"
   }
}

fun AuthScheme.asHocon(): Config {
   return Hocon.encodeToConfig(this)
}

@Serializable
@SerialName("Basic")
data class BasicAuth(
   val username: String,
   val password: String,
) : AuthScheme() {
   override fun sanitized(): SanitizedAuthScheme {
      return copy(password = MASKED_PASSWORD)
   }
}

@Serializable
@SerialName("HttpHeader")
data class HttpHeader(
   val value: String,
   val prefix: String = "Bearer",
   val headerName: String = "Authorization"
) : AuthScheme() {
   override fun sanitized(): SanitizedAuthScheme = copy(value = MASKED_PASSWORD)

   fun prefixedValue(): String {
      return "$prefix $value".trim()
   }
}

@Serializable
@SerialName("QueryParam")
data class QueryParam(
   val parameterName: String,
   val value: String
) : AuthScheme() {
   override fun sanitized(): SanitizedAuthScheme {
      return copy(value = MASKED_PASSWORD)
   }
}

@Serializable
@SerialName("Cookie")
data class Cookie(
   val cookieName: String,
   val value: String
) : AuthScheme() {
   override fun sanitized(): SanitizedAuthScheme {
      return copy(value = MASKED_PASSWORD)
   }
}


@Serializable
@SerialName("OAuth2")
data class OAuth2(
   val accessTokenUrl: String,
   val clientId: String,
   val clientSecret: String,
   val scopes: List<String> = emptyList(),
   val grantType: AuthorizationGrantType,
   val method: AuthenticationMethod = AuthenticationMethod.Basic,


   ) : AuthScheme() {
   enum class AuthorizationGrantType {
      AuthorizationCode,
      RefreshToken,
      ClientCredentials
   }

   enum class AuthenticationMethod {
      Basic,
      Post,
      JWT
   }

   override fun sanitized(): SanitizedAuthScheme {
      return copy(
         clientSecret = MASKED_PASSWORD,
      )
   }
}

