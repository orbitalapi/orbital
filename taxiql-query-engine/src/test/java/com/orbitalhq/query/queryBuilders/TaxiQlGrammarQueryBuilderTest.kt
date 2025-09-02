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
      model CastMember {
         characterName : Character inherits String
         actor : ActorName
      }
      model Actor {
         name : ActorName inherits String
         age : Age inherits Int
         lastUpdated: LastUpdated inherits Instant
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
   fun `generates from null`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("""given { age: Age = null } find { Actor( Age == age ) }""", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( Age == null ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from array of string argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode(
            """given { names : ActorName[] = ["Jimmy", "Jack"] } find { Actor( ActorName in names ) }""",
            schema
         )
      )
      generatedQuery.withoutWhitespace()
         .shouldBe("""find { Actor( ActorName in ["Jimmy","Jack"] ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from array of string with null value argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode(
            """given { names : ActorName[] = ["Jimmy", null, "Jack"] } find { Actor( ActorName in names ) }""",
            schema
         )
      )
      generatedQuery.withoutWhitespace()
         .shouldBe("""find { Actor( ActorName in ["Jimmy",null, "Jack"] ) }""".withoutWhitespace())
   }


   @Test
   fun `generates from string argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode("""given { name : ActorName = "Jimmy" } find { Actor( ActorName == name ) }""", schema)
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( ActorName == "Jimmy" ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from model reference`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode(
            """given {
            |  castMember : CastMember = {
            |     characterName : 'Floppy',
            |     actor : 'Jimmy'
            |  }
            |}
            |find { Actor( ActorName == castMember::ActorName ) }""".trimMargin(), schema
         )
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor( ActorName == "Jimmy" ) }""".withoutWhitespace())
   }

   @Test
   fun `generates from instant argument`() {
      val generatedQuery = generateQuery(
         getQuerySpecNode(
            """given { lastUpdated: LastUpdated = parseDate('2025-05-10T09:30:00Z') }
            | find { Actor( LastUpdated >= lastUpdated ) }""".trimMargin(), schema
         )
      )
      generatedQuery.withoutWhitespace().shouldBe("""find { Actor(
LastUpdated >= "2025-05-10T09:30:00Z"
) }""".withoutWhitespace())
   }

   private fun generateQuery(querySpecNode: Pair<QueryContext, QuerySpecTypeNode>): String {
      val (context, spec) = querySpecNode
      val generated =
         queryBuilder.buildQuery(spec, schema.tableOperations.first().queryOperations.first(), schema, context)
      return generated.entries.single().value.toRawObject() as String
   }
}
