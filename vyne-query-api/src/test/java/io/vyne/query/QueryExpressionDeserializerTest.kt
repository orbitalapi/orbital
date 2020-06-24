package io.vyne.query

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import org.junit.Test

data class QueryContainer(val expression: QueryExpression)

class QueryExpressionDeserializerTest {

   private val jackson = jacksonObjectMapper().registerModule(VyneJacksonModule())

   @Test
   fun given_simpleNameQueryExpression_then_itShouldDeserialzieToTypeNameExpression() {
      val json = """{ "expression" : "foo.bar" }"""
      val result = jackson.readValue<QueryContainer>(json)
      expect(result.expression).to.equal(TypeNameQueryExpression("foo.bar"))
   }

   @Test
   fun given_nameListQueryExpression_then_itShouldDeserializeToNameListExpression() {
      val json = """{ "expression" : [ "foo.bar", "foo.baz" ] }"""
      val result = jackson.readValue<QueryContainer>(json)
      expect(result.expression).to.equal(TypeNameListQueryExpression(listOf("foo.bar", "foo.baz")))
   }
}
