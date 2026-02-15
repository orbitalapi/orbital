package com.orbitalhq.auth.schemes

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValueType
import io.github.config4k.extract
import com.orbitalhq.auth.tokens.AuthToken
import com.orbitalhq.config.getSafeConfigString
import com.orbitalhq.schemas.ServiceName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.encodeToConfig
import mu.KotlinLogging
import java.io.File

@Serializable
data class AuthTokens(
   val authenticationTokens: Map<ServiceName, List<AuthScheme>>
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
         val schemes = tokenConfigs.map { (serviceName, tokenConfigValue) ->
            // Detect whether the config is a single object (old format) or a list (new format)
            val authSchemes: List<AuthScheme> = when (tokenConfigValue.valueType()) {
               ConfigValueType.LIST -> {
                  // New format: array of auth schemes
                  val configList = tokenConfigValue as ConfigList
                  configList.map { item ->
                     require(item is ConfigObject) { "Each auth scheme in list must be an object for service $serviceName" }
                     parseAuthScheme(serviceName, item.toConfig())
                  }
               }
               ConfigValueType.OBJECT -> {
                  // Old format: single auth scheme - wrap in list for backwards compatibility
                  require(tokenConfigValue is ConfigObject) { "Expected ConfigObject for single auth scheme for service $serviceName" }
                  listOf(parseAuthScheme(serviceName, tokenConfigValue.toConfig()))
               }
               else -> error("Auth config for service $serviceName must be either an object or a list, but was ${tokenConfigValue.valueType()}")
            }

            serviceName to authSchemes
         }.toMap()
         return AuthTokens(schemes)
      }

      /**
       * Parses a single auth scheme from config.
       * Handles both new format (with type field) and old format (without type field).
       */
      private fun parseAuthScheme(serviceName: String, tokenConfig: Config): AuthScheme {
         val schemeType = tokenConfig.getString("type").takeIf { tokenConfig.hasPath("type") }
         return if (schemeType == null) {
            // Old format: No type field present
            logger.error { "Configured Auth for service $serviceName does not define a type property - it looks like this is using an old format" }
            val authScheme = tokenConfig.extract<AuthToken>().upgradeToAuthScheme()
            val updatedConfig = authScheme.asHocon()
            logger.info { "Consider upgrading using the following config: \n${updatedConfig.getSafeConfigString()}" }
            authScheme
         } else {
            // New format: Type field present
            when (schemeType) {
               "Basic" -> tokenConfig.extract<BasicAuth>()
               "HttpHeader" -> tokenConfig.extract<HttpHeader>()
               "QueryParam" -> tokenConfig.extract<QueryParam>()
               "OAuth2" -> tokenConfig.extract<OAuth2>()
               "Cookie" -> tokenConfig.extract<Cookie>()
               "MutualTls" -> tokenConfig.extract<MutualTls>()
               else -> error("Unrecognized type of auth scheme: $schemeType")
            }
         }
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
   JsonSubTypes.Type(MutualTls::class, name = "MutualTls")
)
sealed class AuthScheme(
) {
   abstract fun sanitized(): SanitizedAuthScheme

   companion object {
      const val MASKED_PASSWORD = "**************"
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
   val refreshToken: String? = null,

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

@SerialName("MutualTls")
@Serializable
data class MutualTls(
   val keystorePath: String,
   val keystorePassword: String,
   val truststorePath: String,
   val truststorePassword: String
) : AuthScheme() {

   init {
      val configErrors = mutableListOf<String>()

      fun mutualMtlsProp(key:String) = "MutualMtls.$key"
      fun appendPrefixedError(message: String) = configErrors.add("When ${mutualMtlsProp("enabled")} = true, $message")

      if (!File(keystorePath).exists()) {
         appendPrefixedError("${mutualMtlsProp("keystorePath")} is invalid!")
      }

      if (!File(truststorePath).exists()) {
         appendPrefixedError("${mutualMtlsProp("truststorePath")} is invalid!")
      }

      if (configErrors.isNotEmpty()) {
         error(configErrors.joinToString("\n"))
      }
   }
   override fun sanitized(): SanitizedAuthScheme = copy(keystorePassword = MASKED_PASSWORD, truststorePassword= MASKED_PASSWORD)
}

