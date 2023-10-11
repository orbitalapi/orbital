package com.orbitalhq.schemaServer.core.git

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GitRepositoryConfigTest : DescribeSpec({

   describe("redacting urls") {
      it("doesn't modify urls without user info") {
         GitRepositorySpec.redactUrl("http://foo.com/a/b/c?foo=bar#123")
            .shouldBe("http://foo.com/a/b/c?foo=bar#123")
      }
      it("redacts user info") {
         GitRepositorySpec.redactUrl("http://marty:password@foo.com/a/b/c?foo=bar#123")
            .shouldBe("http://mar***@foo.com/a/b/c?foo=bar#123")

      }
   }
})
