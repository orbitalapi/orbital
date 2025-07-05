package com.orbitalhq.copilot

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.orbitalhq.query.QueryParseMetadata
import lang.taxi.query.TaxiQLQueryString
import java.time.Instant


/**
 * When we get a response from the LLM, we break it down into chunks,
 * seperating out the query from the rest.
 * This allows us to provide additional data along with the query, and provide
 * better UI rendering
 */
data class ConversationMessage(
   val message: String,
   val role: ChatMessageRole,
   /**
    * Allows us to show a modified version of the
    * message to the user, that's different from
    * what is sent to the AI
    */
   val displayMessage: String? = null,
   val chunks: List<MessageChunk> = emptyList(),
   val timestamp: Instant = Instant.now()
) {
   @get:JsonIgnore
   val queries: List<CodeBlockMessageChunk> = chunks
      .filterIsInstance<CodeBlockMessageChunk>()


   fun replaceChunks(replacements: List<Pair<MessageChunk, MessageChunk>>): ConversationMessage {
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
      fun messageOnly(role: ChatMessageRole, message: String, displayMessage: String? = null, timestamp: Instant = Instant.now()) =
         ConversationMessage(message, ChatMessageRole.valueOf(role.name), displayMessage, emptyList(), timestamp)
   }
}

data class ConversationSummary(
   val id: String,
   val title: String,
   val startedTimestamp: Instant,
   val mostRecentMessageTimestamp: Instant
)

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.PROPERTY,
   property = "kind"
)
@JsonSubTypes(
   JsonSubTypes.Type(value = ToolMessageChunk::class, name = "Tool"),
   JsonSubTypes.Type(value = ChatMessageChunk::class, name = "Chat"),
   JsonSubTypes.Type(value = CodeBlockMessageChunk::class, name = "CodeBlock"),
   JsonSubTypes.Type(value = CompiledQueryMessageChunk::class, name = "CompiledQuery")
)
interface MessageChunk {
   enum class MessageChunkKind {
      Chat,
      CodeBlock,
      CompiledQuery,
      Tool
   }

   val kind: MessageChunkKind
}

data class ToolMessageChunk(val id: String, val name: String, val toolData: Map<String,Any>) : MessageChunk {
   override val kind: MessageChunk.MessageChunkKind = MessageChunk.MessageChunkKind.Tool
}

data class ChatMessageChunk(val message: String) : MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.Chat
}


data class CodeBlockMessageChunk(val code: TaxiQLQueryString, val language: String) : MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.CodeBlock
}

data class CompiledQueryMessageChunk(val query:QueryParseMetadata) :
   MessageChunk {
   override val kind = MessageChunk.MessageChunkKind.CompiledQuery
}



enum class ChatMessageRole {
   system, user, assistant, tool
}
