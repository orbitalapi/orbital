package com.orbitalhq.spring.http.auth.schemes

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.auth.schemes.*
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.config.SimpleConfigSourceLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class HoconAuthTokensRepositoryTest {
   @TempDir
   @JvmField
   var folder: Path? = null


   @Test
   @Suppress("DEPRECATION")
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
   fun `can lookup a registration key from a token registered with wildcards`() {
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
   "com.bar.Baz" {
      type: Basic
      username: jimmy
      password: password
   }
}
         """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))
      repo.getRegisteredKey("com.foo.bar.MyService").shouldBe("com.foo.*")
      repo.getRegisteredKey("com.foo.MyService").shouldBe("com.foo.*")
      repo.getRegisteredKey("foo.MyService").shouldBeNull()

      // These aren't registered with a wildcard, so should match exactly
      repo.getRegisteredKey("com.bar.Baz").shouldBe("com.bar.Baz")
      repo.getRegisteredKey("com.bar.foo.Baz").shouldBeNull()
      repo.getRegisteredKey("Baz").shouldBeNull()
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

   @Test
   fun `can read an mtls  config`() {
      val keyStorePath =  folder!!.resolve("keystore.jks")
      val trustStorePath =  folder!!.resolve("truststore.jks")
      Files.write(keyStorePath,
         """
                eqwewqewqeqweqwe
               """.trimIndent().encodeToByteArray())
      Files.write(trustStorePath,
              """
                  eqwewqewqeqweqwe
               """.trimIndent().encodeToByteArray())

      configShouldMatch(
         """
            "com.foo.TestService" {
               type: MutualTls
               keystorePath: ${keyStorePath.absolutePathString()}
               keystorePassword: orbital
               truststorePath: ${trustStorePath.absolutePathString()}
               truststorePassword: orbital
            }
      """.trimIndent(), MutualTls(
            keystorePath = keyStorePath.absolutePathString(),
            keystorePassword = "orbital",
            truststorePath = trustStorePath.absolutePathString(),
            truststorePassword = "orbital"
         )
      )
   }


   @Suppress("DEPRECATION")
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
   @Suppress("DEPRECATION")
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
   @Suppress("DEPRECATION")
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
   @Suppress("DEPRECATION")
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
         emptyList(),
         fallback
      )
      val loadedToken = loadingRepo.getAuthScheme("MyService")
      loadedToken.shouldBe(readToken)
   }

   @Test
   fun `can read multiple auth schemes in list format`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.TestService" : [
      {
         type: HttpHeader
         value: token123
         headerName: X-API-Token
      },
      {
         type: QueryParam
         parameterName: apiKey
         value: key456
      },
      {
         type: Cookie
         cookieName: session
         value: sessionValue
      }
   ]
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))
      val schemes = repo.getAuthSchemes("com.foo.TestService")

      schemes.size.shouldBe(3)
      (schemes[0] as HttpHeader).value.shouldBe("token123")
      (schemes[0] as HttpHeader).headerName.shouldBe("X-API-Token")
      (schemes[1] as QueryParam).value.shouldBe("key456")
      (schemes[1] as QueryParam).parameterName.shouldBe("apiKey")
      (schemes[2] as Cookie).value.shouldBe("sessionValue")
      (schemes[2] as Cookie).cookieName.shouldBe("session")
   }

   @Test
   fun `single auth scheme in old format is wrapped in list`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.TestService" {
      type: HttpHeader
      value: token123
   }
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))
      val schemes = repo.getAuthSchemes("com.foo.TestService")

      // Old format should be wrapped in a list
      schemes.size.shouldBe(1)
      (schemes[0] as HttpHeader).value.shouldBe("token123")
   }

   @Test
   @Suppress("DEPRECATION")
   fun `getAuthScheme deprecated method returns first scheme for backwards compatibility`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.TestService" : [
      {
         type: HttpHeader
         value: token123
      },
      {
         type: QueryParam
         parameterName: apiKey
         value: key456
      }
   ]
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val scheme = repo.getAuthScheme("com.foo.TestService")

      scheme.shouldNotBeNull()
      (scheme as HttpHeader).value.shouldBe("token123")
   }

   @Test
   fun `wildcard matching works with multiple auth schemes`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.*" : [
      {
         type: HttpHeader
         value: token123
      },
      {
         type: QueryParam
         parameterName: apiKey
         value: key456
      }
   ]
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val schemes = repo.getAuthSchemes("com.foo.bar.MyService")
      schemes.size.shouldBe(2)
      (schemes[0] as HttpHeader).value.shouldBe("token123")
      (schemes[1] as QueryParam).value.shouldBe("key456")
   }

   @Test
   fun `empty list returned when no auth schemes configured for service`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.TestService" {
      type: HttpHeader
      value: token123
   }
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val schemes = repo.getAuthSchemes("com.bar.OtherService")
      schemes.size.shouldBe(0)
   }

   @Test
   fun `listTokensWithoutCredentials returns sanitized single auth scheme`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.TestService" {
      type: Basic
      username: testuser
      password: secretpassword
   }
   "com.bar.OtherService" {
      type: HttpHeader
      value: supersecrettoken
      headerName: X-API-Key
   }
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val tokensWithoutCreds = repo.listTokensWithoutCredentials()

      tokensWithoutCreds.size.shouldBe(2)
      tokensWithoutCreds["com.foo.TestService"].shouldNotBeNull()
      tokensWithoutCreds["com.foo.TestService"]!!.size.shouldBe(1)

      val basicAuth = tokensWithoutCreds["com.foo.TestService"]!![0] as BasicAuth
      basicAuth.username.shouldBe("testuser")
      basicAuth.password.shouldBe("**************") // Sanitized

      val httpHeader = tokensWithoutCreds["com.bar.OtherService"]!![0] as HttpHeader
      httpHeader.value.shouldBe("**************") // Sanitized
   }

   @Test
   fun `listTokensWithoutCredentials returns sanitized multiple auth schemes`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.MultiAuthService" : [
      {
         type: Basic
         username: user1
         password: pass1
      },
      {
         type: HttpHeader
         value: token123
         headerName: X-API-Token
      },
      {
         type: QueryParam
         parameterName: apiKey
         value: secret456
      }
   ]
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val tokensWithoutCreds = repo.listTokensWithoutCredentials()

      tokensWithoutCreds.size.shouldBe(1)
      val schemes = tokensWithoutCreds["com.foo.MultiAuthService"]
      schemes.shouldNotBeNull()
      schemes!!.size.shouldBe(3)

      // Verify all credentials are sanitized
      val basicAuth = schemes[0] as BasicAuth
      basicAuth.username.shouldBe("user1")
      basicAuth.password.shouldBe("**************")

      val httpHeader = schemes[1] as HttpHeader
      httpHeader.value.shouldBe("**************")

      val queryParam = schemes[2] as QueryParam
      queryParam.value.shouldBe("**************")
   }

   @Test
   fun `listTokensWithoutCredentials works with wildcard patterns`() {
      val loader = SimpleConfigSourceLoader(
         VersionedSource(
            "auth.conf",
            "1.0.0",
            """
authenticationTokens {
   "com.foo.*" {
      type: HttpHeader
      value: wildcardtoken
   }
   "com.bar.SpecificService" : [
      {
         type: Basic
         username: admin
         password: adminpass
      },
      {
         type: QueryParam
         parameterName: key
         value: value123
      }
   ]
}
            """.trimIndent()
         )
      )
      val repo = HoconAuthTokensRepository(listOf(loader))

      val tokensWithoutCreds = repo.listTokensWithoutCredentials()

      tokensWithoutCreds.size.shouldBe(2)

      // Wildcard should be included
      tokensWithoutCreds["com.foo.*"].shouldNotBeNull()
      tokensWithoutCreds["com.foo.*"]!!.size.shouldBe(1)

      // Specific service with multiple schemes
      tokensWithoutCreds["com.bar.SpecificService"].shouldNotBeNull()
      tokensWithoutCreds["com.bar.SpecificService"]!!.size.shouldBe(2)
   }


}
