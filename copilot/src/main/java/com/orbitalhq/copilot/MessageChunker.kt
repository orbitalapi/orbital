package com.orbitalhq.copilot

/**
 * Takes a response from an LLM, and breaks it into chunks
 */
class MessageChunker {
   fun chunkMessage(rawMessage: String, role: OpenAiChatMessage.Role): ConversationMessage {
      val chunks = mutableListOf<MessageChunk>()
      val regex = Regex("""```taxi\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)

      var lastEnd = 0
      regex.findAll(rawMessage).forEach { matchResult ->
         val start = matchResult.range.first
         val end = matchResult.range.last + 1

         // Add the chat message chunk if there's text before the query
         if (lastEnd < start) {
            val chatText = rawMessage.substring(lastEnd, start).trim()
            if (chatText.isNotEmpty()) {
               chunks.add(ChatMessageChunk(chatText))
            }
         }

         // Add the query message chunk
         val queryText = matchResult.groupValues[1].trim()
         chunks.add(QueryMessageChunk(queryText))

         lastEnd = end
      }

      // Add any remaining chat message chunk after the last query
      if (lastEnd < rawMessage.length) {
         val remainingChatText = rawMessage.substring(lastEnd).trim()
         if (remainingChatText.isNotEmpty()) {
            chunks.add(ChatMessageChunk(remainingChatText))
         }
      }

      return ConversationMessage(
         rawMessage, role, displayMessage = null,
         chunks = chunks
      )
   }
}
