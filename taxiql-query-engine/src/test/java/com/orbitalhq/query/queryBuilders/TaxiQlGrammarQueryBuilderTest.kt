package com.orbitalhq.query.queryBuilders

import com.orbitalhq.models.functions.stdlib.withoutWhitespace
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.getQuerySpecNode
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class TaxiQlGrammarQueryBuilderTest {

   lateinit var queryBuilder: TaxiQlGrammarQueryBuilder;

   @Before
   fun setup() {
      queryBuilder = TaxiQlGrammarQueryBuilder()
   }

   val schema = TaxiSchema.fromStrings(
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
      model Actor {
         name : ActorName inherits String
         age : Age inherits Int
      }
      service DbService {
         table actors : Actor[]
      }
   """.trimIndent()
   )

   @Test
   fun `generates from string literal`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("find { Actor( ActorName == 'Jimmy' ) }", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( ActorName == "Jimmy" ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from numeric literal`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("find { Actor( Age == 32 ) }", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( Age == 32 ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from numeric argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("""given { age: Age = 32 } find { Actor( Age == age ) }""", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( Age == 32 ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from string argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("""given { name : ActorName = "Jimmy" } find { Actor( ActorName == name ) }""", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( ActorName == "Jimmy" ) }""".withoutWhitespace())
   }

   private fun generateQuery(querySpecNode: Pair<QueryContext, QuerySpecTypeNode>): String {
      val (context, spec) = querySpecNode
      val generated = queryBuilder.buildQuery(spec, schema.tableOperations.first().queryOperations.first(), schema, context)
      return generated.entries.single().value.toRawObject() as String
   }
}
