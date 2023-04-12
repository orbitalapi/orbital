package io.vyne.query.runtime.core

import io.vyne.query.chat.ChatGptQuery
import io.vyne.query.chat.ChatQueryParser
import io.vyne.query.chat.TaxiQlGenerator
import io.vyne.schema.api.SchemaProvider
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryChatService(private val parser: ChatQueryParser, private val schemaProvider: SchemaProvider) {

   @PostMapping("/api/query/chat/parse")
   fun parseChatQuery(@RequestBody queryText: String): ChatParseResult {
      val schema = schemaProvider.schema
      val chatGptQuery = parser.parseToChatQuery(schema, queryText)
      val taxi = TaxiQlGenerator.convertToTaxi(chatGptQuery, schema)
      return ChatParseResult(
         queryText,
         chatGptQuery, taxi
      )
   }

}

data class ChatParseResult(
   val queryText: String,
   val chatGptQuery: ChatGptQuery,
   val taxi: String
)
