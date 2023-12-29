package com.orbitalhq.query.runtime.core

import com.orbitalhq.query.chat.TaxiQlGenerationResult
import com.orbitalhq.query.chat.ChatQueryParser
import com.orbitalhq.schema.api.SchemaProvider
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryChatService(private val parser: ChatQueryParser, private val schemaProvider: SchemaProvider) {

   @PostMapping("/api/query/chat/parse")
   fun parseChatQuery(@RequestBody queryText: String): ChatParseResult {
      val schema = schemaProvider.schema
      val generationResult = parser.generateQueryFromText(schema, queryText)
      return ChatParseResult(
         queryText,
         generationResult,
         generationResult.taxi
      )
   }

}

data class ChatParseResult(
   val queryText: String,
   val chatGptQuery: TaxiQlGenerationResult,
   val taxi: String
)
