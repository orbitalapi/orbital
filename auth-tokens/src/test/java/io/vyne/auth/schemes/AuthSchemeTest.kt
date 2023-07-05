package io.vyne.auth.schemes

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import io.kotest.matchers.shouldBe
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.encodeToConfig
import org.junit.Test

class AuthSchemeTest {

   @Test
   fun `can read and write to HOCON`() {
      val tokens = AuthTokens(
         mapOf(
            "com.foo.BasicService" to BasicAuth("jimmy", "password"),
            "com.foo.HeaderService" to HttpHeader("header", "wine"),
            "com.foo.QueryParamService" to QueryParam("auth", "wine"),
            "com.bar.baz.OauthService" to OAuth2(
               "http://foo.com",
               "clientId",
               "secret",
               listOf("name", "photo"),
               OAuth2.AuthorizationGrantType.AuthorizationCode
            )
         )
      )

      val config = Hocon.encodeToConfig(tokens)
      val hocon = config.root().render(
         ConfigRenderOptions.defaults()
            .setFormatted(true)
            .setComments(true)
            .setOriginComments(false)
      )

      val deserializedConfig = ConfigFactory
         .parseString(hocon, ConfigParseOptions.defaults())

      val deserializedTokens = AuthTokens.fromConfig(deserializedConfig)
      deserializedTokens.shouldBe(tokens)
   }
}
