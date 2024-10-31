package com.orbitalhq.cockpit.core.security.authorisation

import com.orbitalhq.utils.log
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.security.authorisation")
class VyneAuthorisationConfig {
   val roleDefinitionsFile: Path = Paths.get("config/roles.conf")
}

/**
 * Defines which type of token returned from the OIDC Authentication service
 * should be sent back to our server to verify the user.
 *
 * Default (per the spec) is Access.
 * In AWS Cognito, the Access token is pretty sparse, and the details we need
 * are in the Id token.
 *
 * Setting this informs the UI which token to send in the Authorization header
 */
enum class IdentityTokenKind {
   Access,
   Id
}

/**
 * Configuration For SAML Authentication.
 * @param enabled Enables the Saml Authentication flow in Orbital.
 * @param keyStorePath Path of the key store file on Orbital host. If key store does not exist. Orbital will create on at the specified location.
 * @param privateKeyPassword Password for the private key stored in the given key store.
 * @param idpMetadataFilePath File Path for Idp metadata.
 * @param serviceProviderMetadataResourcePath File Path to store the SP metadata that we will generate for Orbital.
 * @param callbackBaseUrl after a successful login, the identity provider will redirect the user back to the application on the callback URL which is defined as callbackBaseUrl/callback
 *  so, assume that orbital is accessible from okta as https://orbital.company.com
 *  in this case callbackBaseUrl needs to be https://orbital.company.com and the callback url setting in okta needs to be https://orbital.company.com/callback
 * @param serviceProviderEntityId By default, the entity ID of your application (the Service Provider) will be equals to the callback URL. But you can force your own entity ID with the serviceProviderEntityId parameter
 * @param maximumAuthenticationLifetime  by default, the SAML client will accept assertions based on a previous authentication for one hour. If you want to change this behavior, set the maximumAuthenticationLifetime parameter
 */
@ConfigurationProperties(prefix = "vyne.security.saml")
data class VyneSamlConfig(
   val enabled: Boolean = false,
   val keyStorePath: String = "./orbital-saml.jks",
   val keyStorePassword: String? = null,
   val privateKeyPassword: String? = null,
   val idpMetadataFilePath: String? = null,
   val serviceProviderMetadataResourcePath: String = "./sp-metadata.xml",
   val callbackBaseUrl: String = "http://localhost:9022",
   val serviceProviderEntityId: String? = null,
   val maximumAuthenticationLifetime: Long = 3600
) {
   init {
      val configErrors = mutableListOf<String>()

      fun samlProp(key:String) = "vyne.security.saml.$key"
      fun appendPrefixedError(message: String) = configErrors.add("When ${samlProp("enabled")} = true, $message")

      if (enabled) {
         if (serviceProviderEntityId == null) appendPrefixedError("${samlProp("serviceProviderEntityId")} must be set")
         if (idpMetadataFilePath == null) appendPrefixedError("${samlProp("idpMetadataFilePath")} must be set")
         if (keyStorePassword == null) appendPrefixedError("${samlProp("keyStorePassword")} must be set")
         if (privateKeyPassword == null) appendPrefixedError("${samlProp("privatePassword")} must be set")
         if (configErrors.isNotEmpty()) {
            error(configErrors.joinToString("\n"))
         }
      }
   }
}

enum class ClientAuthenticationType {
   ClientSecretPost,
   ClientSecretBasic
}

// configuration class annotation need to use kebab-case, otherwise spring gives prefix must be in canonical form in Intellij
@ConfigurationProperties(prefix = "vyne.security.open-idp")
data class VyneOpenIdpConnectConfig(
   val enabled: Boolean = false,
   // Open Idp issuer Url
   val issuerUrl: String? = null,
   // The client Id defined in Idp for Orbital.
   // Null if enabled = false
   val clientId: String? = null,
   // Scopes defined in Idp
   val scope: String = "openid profile email offline_access",
   // Require login via https
   val requireHttps: Boolean = true,
   val accountManagementUrl: String? = null,
   val orgManagementUrl: String? = null,
   val jwksUri: String? = null,
   val roles: JwtRolesConfig = JwtRolesConfig(),
   val identityTokenKind: IdentityTokenKind = IdentityTokenKind.Access,
   // Some Authorisation servers, like Spring Authorization Server, does not issue
   // refresh tokens in Code Flow. For these cases, we'd like to set 'useSilentRefresh'
   // Config parameter of the angular-oauth2-oidc to true
   // see: https://manfredsteyer.github.io/angular-oauth2-oidc/docs/classes/AuthConfig.html#useSilentRefresh
   val refreshTokensDisabled: Boolean = false,
   /**
    * The url to load the oidc discovery document from.
    * Normally is inferred from the issuerUrl
    * (ie., ${issuerUrl}/.well-known/openid-configuration)
    * However, some IDP's use a custom discovery url. (Azure).
    */
   val oidcDiscoveryUrl: String?,

   /**
    * Defines the clientId for the role
    * that Orbital uses to execute streaming queries
    * and background tasks that are executed without a user context
    */
   val executorRoleClientId: String? = null,
   /**
    * Defines the clientSecret for the role
    * that Orbital uses to execute streaming queries
    * and background tasks that are executed without a user context
    */
   val executorRoleClientSecret: String? = null,

   /**
    * Defines the Url to fetch the access token by using `executorRoleClientId` and `executorRoleClientSecret`
    * If it is null then we'll try to extract the `token_endpoint` value from http://OPEN_IDP_SERVER/.well-known/openid-configuration
    */
   val executorRoleTokenUrl: String? = null,

   /**
    * Defines the Authentication Type to fetch the access token for Executor Role.
    */
   val executorRoleAuthenticationType: ClientAuthenticationType = ClientAuthenticationType.ClientSecretPost,

   /**
    * Optional scopes that needs to be passed when asking for the access token for the Executor Role.
    */
   val executorRoleScopes: String? = null,
   /**
    * Expected audience (aud) value in the provided Jwt token
    * By default this value is null which means that audience verification is disabled.
    * When Populated a custom OAuth2TokenValidator will be instantiated to verify Jwt token's aud
    * value against this.
    */
   val audience: String? = null

) {
   init {
      val configErrors = mutableListOf<String>()

      fun idpProperty(key:String) = "vyne.security.open-idp.$key"
      fun appendPrefixedError(message: String) = configErrors.add("When ${idpProperty("enabled")} = true, $message")

      if (enabled) {
         log().info("Open IDP is enabled, using settings: $this")
         if (clientId == null) appendPrefixedError("${idpProperty("client-id")} must be set")
         if (issuerUrl == null && oidcDiscoveryUrl == null) appendPrefixedError("either ${idpProperty("oidc-discovery-url")} or ${idpProperty("issuer-url")} must be set")
         if (configErrors.isNotEmpty()) {
            error(configErrors.joinToString("\n"))
         }
      }
   }
}


/**
 * Additional config settings for how to extract roles from OIDC JWT's.
 */
data class JwtRolesConfig(
   // See: KeycloakRolesExtractor.KeycloakJwtKind |PropelAuthClaimsExtractor.PropelAuthJwtKind | PathRolesExtractor
   val format: String = KeycloakRolesExtractor.KeycloakJwtRolesFormat,
   val path: String? = null
) {
   init {
       if (format == SimplePathBasedRolesExtractor.PathJwtKind && path == null) {
          error("When vyne.security.open-idp.roles.format = ${SimplePathBasedRolesExtractor.PathJwtKind} you must also specify vyne.security.open-idp.roles.path indicating the path within the token to read the roles from")
       }
   }
}
