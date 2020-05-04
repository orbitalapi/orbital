package io.vyne.query

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import org.junit.Test

class QueryExpressionSerializerTest {
   private val jackson = jacksonObjectMapper().registerModule(VyneJacksonModule())


   @Test
   fun serializesCorrectly() {
      val container = QueryContainer(TypeNameListQueryExpression(listOf("foo", "bar")))
      val json = jackson.writeValueAsString(container)
      val deser = jackson.readValue<QueryContainer>(json)
      expect(deser).to.equal(container)
   }
}
