package io.vyne.spring.http.auth.schemes

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.vyne.VersionedSource
import io.vyne.config.SimpleHoconLoader
import org.junit.jupiter.api.Test

class HoconAuthTokensRepositoryTest {

   @Test
   fun `when a token is defined with a wildcard then it is returned`() {
      val loader = SimpleHoconLoader(
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
}
