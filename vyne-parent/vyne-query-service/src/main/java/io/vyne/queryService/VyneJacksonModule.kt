package io.vyne.queryService

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.query.QueryExpression
import io.vyne.query.TypeNameListQueryExpression
import io.vyne.query.TypeNameQueryExpression

class VyneJacksonModule : SimpleModule("Vyne") {
   init {
      addDeserializer(QueryExpression::class.java, QueryExpressionDeserializer())
   }
}

class QueryExpressionDeserializer(private val mapper:ObjectMapper = jacksonObjectMapper()) : JsonDeserializer<QueryExpression>() {

   override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): QueryExpression {
      val tree = parser.readValueAsTree<JsonNode>()
      return when (tree) {
         is TextNode -> TypeNameQueryExpression(mapper.convertValue(tree))
         is ArrayNode -> TypeNameListQueryExpression(mapper.convertValue(tree))
         // TODO : GraphQL parsing
         else -> TODO("Unable to create QueryExpression from json $tree")
      }
   }

}
