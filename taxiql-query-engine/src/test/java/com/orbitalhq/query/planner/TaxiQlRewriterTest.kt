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

      it("should prepend an annotation if not present") {
         val original = """find { Foo } as { bar : Bar }"""
         val updated = TaxiQlRewriter().appendQueryAnnotationIfNotPresent(original, "@Cached")
         updated.shouldBe("""@Cached
            |$original
         """.trimMargin())
      }
      it("should not prepend an annotation if already present") {
         val original = """@Cached
            |find { Foo } as { bar : Bar }""".trimMargin()
         val updated = TaxiQlRewriter().appendQueryAnnotationIfNotPresent(original, "@Cached")
         updated.shouldBe(original)
      }
      it("should prepend an annotation if not present but other annotations are") {
         val original = """@Something
            |find { Foo } as { bar : Bar }""".trimMargin()
         val updated = TaxiQlRewriter().appendQueryAnnotationIfNotPresent(original, "@Cached")
         updated.shouldBe("""@Something
            |@Cached
            |find { Foo } as { bar : Bar }""".trimMargin())
      }
   }
})
