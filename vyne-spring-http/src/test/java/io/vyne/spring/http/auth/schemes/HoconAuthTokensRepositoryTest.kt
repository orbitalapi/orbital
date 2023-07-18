package io.vyne.spring.http.auth.schemes

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.vyne.VersionedSource
import io.vyne.auth.schemes.BasicAuth
import io.vyne.config.SimpleConfigSourceLoader
import org.junit.jupiter.api.Test

class HoconAuthTokensRepositoryTest {

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
}
