package io.vyne.query.queryBuilders

import io.vyne.query.getQuerySpecNode
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test

class VyneQlGrammarQueryBuilderTest {
   val schema = TaxiSchema.fromStrings(
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
         type FirstName inherits String
         type Age inherits Int
         model Person {
            firstName : FirstName
            age : Age
         }
      """.trimIndent())

   lateinit var builder:VyneQlGrammarQueryBuilder
   @Before
   fun setup() {
      builder = VyneQlGrammarQueryBuilder()
   }
   @Test
   fun buildsVyneQlForEquals() {
      val querySpecNode = getQuerySpecNode("findAll { Person[]( FirstName = 'Jimmy' ) }", schema)
      builder.buildVyneQl(querySpecNode)
   }
}
