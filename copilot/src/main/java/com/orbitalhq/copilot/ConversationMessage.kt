package com.orbitalhq.copilot

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.query.ParsedQuery
import lang.taxi.query.TaxiQLQueryString

/**
 * When we get a response from the LLM, we break it down into chunks,
 * seperating out the query from the rest.
 * This allows us to provide additional data along with the query, and provide
 * better UI rendering
 */
data class ConversationMessage(
   val message: String,
   val role: OpenAiChatMessage.Role,
   /**
    * Allows us to show a modified version of the
    * message to the user, that's different from
    * what is sent to the AI
    */
   val displayMessage: String? = null,
   val chunks: List<MessageChunk> = emptyList()
) {
   @get:JsonIgnore
   val queries: List<QueryMessageChunk> = chunks
      .filterIsInstance<QueryMessageChunk>()

   fun toOpenAiChatMessage(): OpenAiChatMessage {
      return OpenAiChatMessage(role, message)
   }

   fun replaceChunks(replacements: List<Pair<MessageChunk,MessageChunk>>): ConversationMessage {
      val mutableChunks = chunks.toMutableList()
      replacements.forEach { (oldChunk, newChunk) ->
         mutableChunks[chunks.indexOf(oldChunk)] = newChunk
      }

      return copy(chunks = mutableChunks)
   }



   override fun toString(): String {
      return "Message Chunk: [${role.name}]: $message"
   }

   companion object {
      fun messageOnly(role: OpenAiChatMessage.Role, message: String, displayMessage: String? = null) =
         ConversationMessage(message, OpenAiChatMessage.Role.valueOf(role.name), displayMessage, emptyList())
   }
}

data class Conversation(
   val messages: List<ConversationMessage>
) {
   fun appendMessage(message: ConversationMessage): Conversation {
      return Conversation(messages + message)
   }

   fun appendMessage(role: OpenAiChatMessage.Role, content: String): Conversation {
      return appendMessage(
         ConversationMessage(
            content, role
         )
      )
   }

   fun replaceLastMessage(validatedResponse: ConversationMessage):Conversation {
      return this.copy(messages = this.messages.dropLast(1) + validatedResponse)
   }
}

interface MessageChunk {
   enum class MessageChunkKind {
      Chat,
      Query,
      CompiledQuery
   }

   val kind: MessageChunkKind
}

data class ChatMessageChunk(val message: String) : MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.Chat
}

/**
 * A raw taxiQL message. This is the first pass of what we get back from the
 */
data class QueryMessageChunk(val taxi: TaxiQLQueryString) : MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.Query
}

data class CompiledQueryMessageChunk(val query:ParsedQuery) :
   MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.CompiledQuery
}

