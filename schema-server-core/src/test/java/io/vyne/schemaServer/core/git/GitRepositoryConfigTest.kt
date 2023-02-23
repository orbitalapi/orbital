package io.vyne.schemaServer.core.git

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GitRepositoryConfigTest : DescribeSpec({

   describe("redacting urls") {
      it("doesn't modify urls without user info") {
         GitRepositoryConfig.redactUrl("http://foo.com/a/b/c?foo=bar#123")
            .shouldBe("http://foo.com/a/b/c?foo=bar#123")
      }
      it("redacts user info") {
         GitRepositoryConfig.redactUrl("http://marty:password@foo.com/a/b/c?foo=bar#123")
            .shouldBe("http://mar***@foo.com/a/b/c?foo=bar#123")

      }
   }
})
