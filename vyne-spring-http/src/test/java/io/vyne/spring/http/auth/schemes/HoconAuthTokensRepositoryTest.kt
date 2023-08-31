package io.vyne.spring.http.auth.schemes

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.vyne.PackageIdentifier
import io.vyne.VersionedSource
import io.vyne.auth.schemes.*
import io.vyne.config.FileConfigSourceLoader
import io.vyne.config.SimpleConfigSourceLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class HoconAuthTokensRepositoryTest {
   @TempDir
   @JvmField
   var folder: Path? = null

   @Test
   fun `when a token is defined with a wildcard then it is returned`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.*" {
      type: Basic
      username: jimmy
      password: password
   }
}
         """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))
      repo.getAuthScheme("com.foo.bar.MyService").shouldNotBeNull()
      repo.getAuthScheme("com.foo.MyService").shouldNotBeNull()
      repo.getAuthScheme("foo.MyService").shouldBeNull()
   }

   @Test
   fun `can read a basic auth token config`() {
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: Basic
               username: jimmy
               password: password
            }
      """.trimIndent(), BasicAuth("jimmy", "password")
      )
   }

   @Test
   fun `can read a http header auth token config`() {
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: HttpHeader
               value: letMeIn
            }
      """.trimIndent(), HttpHeader(value = "letMeIn")
      )
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: HttpHeader
               prefix: "Token",
               headerName: Auth
               value: letMeIn
            }
      """.trimIndent(), HttpHeader(value = "letMeIn", prefix = "Token", headerName = "Auth")
      )
   }

   @Test
   fun `can read a query param auth token config`() {
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: QueryParam
               parameterName: authKey
               value: letMeIn
            }
      """.trimIndent(), QueryParam(value = "letMeIn", parameterName = "authKey")
      )
   }

   @Test
   fun `can read a cookie auth token config`() {
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: Cookie
               cookieName: authKey
               value: letMeIn
            }
      """.trimIndent(), Cookie(value = "letMeIn", cookieName = "authKey")
      )
   }

   @Test
   fun `can read an oauth2 auth token config`() {
      configShouldMatch(
         """
            "com.foo.TestService" {
               type: OAuth2
               accessTokenUrl: "https://auth.com/tokens"
               clientId: ABC
               clientSecret: DEF
               scopes: [ "profile" , "image" ]
               grantType: AuthorizationCode
               method: Post
            }
      """.trimIndent(), OAuth2(
            accessTokenUrl = "https://auth.com/tokens",
            clientId = "ABC",
            clientSecret = "DEF",
            scopes = listOf("profile", "image"),
            grantType = OAuth2.AuthorizationGrantType.AuthorizationCode,
            method = OAuth2.AuthenticationMethod.Post
         )
      )
   }


   private fun configShouldMatch(config: String, auth: AuthScheme, serviceName: String = "com.foo.TestService") {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   $config
}
         """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))
      val loaded = repo.getAuthScheme(serviceName)
      loaded.shouldNotBeNull()
      loaded.shouldBe(auth)
   }

   @Test
   fun `when env vars are provided from another loader they are resolved`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "MyService" {
      type: Basic
      username: jimmy
      password: ${'$'}{thePassword}
   }
}
         """.trimIndent()
         )
      )
      val variablesLoader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
thePassword: hello
         """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(variablesLoader, loader))
      val authScheme = repo.getAuthScheme("MyService")!!
      (authScheme as BasicAuth).password.shouldBe("hello")
   }

   @Test
   fun `can save a token`() {
      val packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
      val configFilePath = folder!!.resolve("auth.conf")
      val savingRepo = HoconAuthTokensRepository(
         listOf(
            FileConfigSourceLoader(
               configFilePath,
               packageIdentifier = packageIdentifier,
               failIfNotFound = false
            )
         )
      )
      val token = BasicAuth("jimmy", "letmein")
      savingRepo.saveToken(packageIdentifier, "MyService", token)
      Files.exists(configFilePath).shouldBeTrue()
      val saved = configFilePath.readText()
      saved.shouldNotBeNull()

      // Now load it back
      val loadingRepo = HoconAuthTokensRepository(
         listOf(
            FileConfigSourceLoader(
               configFilePath,
               packageIdentifier = packageIdentifier,
               failIfNotFound = true
            )
         )
      )
      val loadedToken = loadingRepo.getAuthScheme("MyService")
      loadedToken.shouldBe(token)
   }

   @Test
   fun `when saving a token with an env variable the unresolved value is saved`() {
      val packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
      val configFilePath = folder!!.resolve("auth.conf")
      val fallback = ConfigFactory.parseMap(
         mapOf(
            "password" to "letmein"
         )
      )
      val savingRepo = HoconAuthTokensRepository(
         listOf(
            FileConfigSourceLoader(
               configFilePath,
               packageIdentifier = packageIdentifier,
               failIfNotFound = false
            )
         ),
         fallback = fallback
      )
      val passwordAsVariable = "${"$"}{password}"
      val token = BasicAuth("jimmy", passwordAsVariable)
      savingRepo.saveToken(packageIdentifier, "MyService", token)

      // When reading out of the repo, the env-var should be substitued
      val readToken = savingRepo.getAuthScheme("MyService") as BasicAuth
      readToken.password.shouldBe("letmein")

      Files.exists(configFilePath).shouldBeTrue()
      val saved = configFilePath.readText()
      saved.shouldNotBeNull()

      saved.shouldNotContain("letmein")
      saved.shouldContain(passwordAsVariable)

      // Now load it back
      val loadingRepo = HoconAuthTokensRepository(
         listOf(
            FileConfigSourceLoader(
               configFilePath,
               packageIdentifier = packageIdentifier,
               failIfNotFound = true
            )
         ),
         fallback
      )
      val loadedToken = loadingRepo.getAuthScheme("MyService")
      loadedToken.shouldBe(readToken)
   }


}
