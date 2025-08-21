package com.orbitalhq

import com.orbitalhq.annotations.streaming.StreamingQueryAnnotations
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.taxi.TaxiSchema
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

   @Test
   fun `can set a different consumer group for a named query`() {
      val (vyne, _) = testVyne(TaxiSchema.fromStrings(listOf("""model Person""", StreamingQueryAnnotations.schema)))
      val (query, options) = vyne.parseQuery(
         """
         |import com.orbitalhq.streams.StreamConsumer
         |@StreamConsumer( id = "my-consumer" )
         |query MySavedQuery {
         |   find { Person }
         |}
      """.trimMargin()
      )
      options.streamConsumerId.shouldBe("my-consumer")
   }

   @Test
   fun `can set a different consumer group for a query`() {
      val (vyne, _) = testVyne(TaxiSchema.fromStrings(listOf("""model Person""", StreamingQueryAnnotations.schema)))
      val (query, options) = vyne.parseQuery(
         """
         |import com.orbitalhq.streams.StreamConsumer
         |@StreamConsumer( id = "my-consumer" )
         |find { Person }
         |
         |
      """.trimMargin()
      )
      options.streamConsumerId.shouldBe("my-consumer")
   }

}
