package com.orbitalhq.query.planner

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*

class TaxiQlRewriterTest : DescribeSpec({
   describe("rewriting taxiQL queries") {
      it("should append a stream source") {
         val originalQuery = """stream { Foo } as {
    bar : Bar
   }[]"""
         val updated = TaxiQlRewriter().appendStreamSource(originalQuery.trimMargin(), "Blinky")
         updated.shouldBe("""stream { Foo | Blinky } as {
    bar : Bar
   }[]""")
      }
      it("should append a stream source when there's already two") {
         val originalQuery = """stream { Foo | Blinky } as {
    bar : Bar
   }[]"""
         val updated = TaxiQlRewriter().appendStreamSource(originalQuery.trimMargin(), "Baz")
         updated.shouldBe("""stream { Foo | Blinky | Baz } as {
    bar : Bar
   }[]""")
      }
      it("should append a stream source when streams are across multiple lines") {
         val originalQuery = """stream {
            Foo |
            Blinky
      } as {
    bar : Bar
   }[]"""
         val updated = TaxiQlRewriter().appendStreamSource(originalQuery.trimMargin(), "Baz")
         updated.shouldBe("""stream {
            Foo |
            Blinky | Baz
      } as {
    bar : Bar
   }[]""")
      }
   }
})
