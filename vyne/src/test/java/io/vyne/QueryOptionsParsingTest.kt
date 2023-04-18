package io.vyne

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.vyne.schemas.QueryOptions
import org.junit.jupiter.api.Test

class QueryOptionsParsingTest {

   @Test
   fun `parses query options`() {
      val (vyne, _) = testVyne("""model Person""")
      val (query, options) = vyne.parseQuery(
         """@OmitNulls
         |query MyQuery {
         |  find { Person }
         |}
      """.trimMargin()
      )
      options.omitNulls.shouldBeTrue()
   }

   @Test
   fun `uses defaults when options not provided`() {
      val (vyne, _) = testVyne("""model Person""")
      val (query, options) = vyne.parseQuery(
         """
         |find { Person }
         |
      """.trimMargin()
      )
      options.shouldBe(QueryOptions()) // default
   }
}
