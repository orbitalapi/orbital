package io.vyne.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class VyneJacksonModule : SimpleModule("Vyne") {
   init {
//      addSerializer(QueryExpression::class.java, QueryExpressionSerializer())
//      addDeserializer(QueryExpression::class.java, QueryExpressionDeserializer())
   }
}

class QueryExpressionDeserializer(private val mapper: ObjectMapper = jacksonObjectMapper()) : JsonDeserializer<QueryExpression>() {

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

class QueryExpressionSerializer : JsonSerializer<QueryExpression>() {
   override fun serialize(value: QueryExpression, gen: JsonGenerator, serializers: SerializerProvider) {
      when (value) {
         is TypeNameQueryExpression -> gen.writeString(value.typeName)
         is TypeNameListQueryExpression -> {
            gen.writeStartArray()
            value.typeNames.forEach { gen.writeString(it) }
            gen.writeEndArray()
         }
         else -> TODO("Serialization for this type of QueryExpression not supported")
      }
   }

}
