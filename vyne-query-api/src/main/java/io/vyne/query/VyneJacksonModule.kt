package io.vyne.query

import com.fasterxml.jackson.databind.module.SimpleModule

class VyneJacksonModule : SimpleModule("Vyne") {
   init {
//      addSerializer(QueryExpression::class.java, QueryExpressionSerializer())
//      addDeserializer(QueryExpression::class.java, QueryExpressionDeserializer())
   }
}

//class QueryExpressionDeserializer(private val mapper: ObjectMapper = jacksonObjectMapper()) : JsonDeserializer<QueryExpression>() {
//
//   override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): QueryExpression {
//      val tree = parser.readValueAsTree<JsonNode>()
//      return when (tree) {
//         is TextNode -> TypeNameQueryExpression(mapper.convertValue(tree))
//         is ArrayNode -> TypeNameListQueryExpression(mapper.convertValue(tree))
//         // TODO : GraphQL parsing
//         else -> TODO("Unable to create QueryExpression from json $tree")
//      }
//   }
//}
