package com.orbitalhq

import com.orbitalhq.schemas.QueryOptions
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QueryOptionsParsingTest {
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
