package io.vyne.schemaServer.core.git.providers

import com.winterbe.expekt.should
import org.junit.Test

class GithubProviderTest  {
   @Test
   fun `can guess repo name from https uri`() {
      GithubProvider.repositoryNameFromUri("https://github.com/TheMangoFactory/bakehouse.git")
         .should.equal("TheMangoFactory/bakehouse")
   }

   @Test
   fun `can guess repo name from ssh uri`() {
      GithubProvider.repositoryNameFromUri("git@github.com:TheMangoFactory/bakehouse.git")
         .should.equal("TheMangoFactory/bakehouse")
   }
}
