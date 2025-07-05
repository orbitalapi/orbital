package com.orbitalhq.copilot

import com.orbitalhq.SourcePackage
import com.orbitalhq.utils.SourcePackageCompression
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Initiates a new session, providing the current schema
 */
data class CreateConversationSessionMessage(
   val sourcePackageZip: ByteArray,
) {
   constructor(sources: List<SourcePackage>) : this(
      SourcePackageCompression.compressSourcePackages(sources)
   )

   fun sources(): List<SourcePackage> {
      return SourcePackageCompression.decompressSourcePackages(this.sourcePackageZip)
   }
}

interface CopilotConversationApi {

   @PostExchange("/api/copilot/session")
   fun startSession(@RequestBody message: CreateConversationSessionMessage): ConversationSummary

   @GetExchange("/api/copilot/session/{sessionId}")
   fun getSessionMessages(@PathVariable sessionId: String): List<ConversationMessage>

   @GetExchange("/api/copilot/session/{sessionId}/events")
   fun getSessionChatEvents(@PathVariable sessionId: String): List<ChatEvent>


   // This was the initial impl., where state is effectivley held on the client
   @PostExchange("/api/copilot/session/{sessionId}/messages")
   @Deprecated("Conversation state is now handled on copilot", replaceWith = ReplaceWith("submitMessageToConversation"))
   fun submitConversation(@RequestBody messages: List<ConversationMessage>, @PathVariable("sessionId") sessionId: String): Mono<ConversationMessage>

   // Subsequent impl., where state is held on the server
   // Reduces serialization complexity with things like Tool messages, which we might not
   // want to leak to the client
   @PostExchange("/api/copilot/session/{sessionId}/message")
   fun submitMessageToConversation(@RequestBody message: ConversationMessage, @PathVariable("sessionId") sessionId: String): Mono<ConversationMessage>

   @PostExchange("/api/copilot/session/{sessionId}/message/stream")
   fun submitMessageToConversationStream(@RequestBody message: ConversationMessage, @PathVariable("sessionId") sessionId: String): Flux<ServerSentEvent<ChatEvent>>


   @PostExchange("/api/copilot/session/{sessionId}/message-with-attachments", accept = [MediaType.MULTIPART_FORM_DATA_VALUE])
   fun submitMessageWithAttachments(
      @RequestPart("message") messageJson: String,
      @RequestPart("files") files: Flux<FilePart>,
      @PathVariable("sessionId") sessionId: String
   ): Mono<String>

   @PostExchange("/api/copilot/session/{sessionId}/title")
   fun renameConversation(@PathVariable sessionId: String): Mono<ConversationSummary>

   /**
    * Used to submit the responses to a user interaction response promtoed by an earlier conversation.
    */
   @PostExchange("/api/copilot/session/{sessionId}/userResponse")
   fun submitUserToolResponse(@RequestBody toolCallResponses: UserInteractionToolResponseSubmission, @PathVariable("sessionId") sessionId: String): ConversationMessage

   @GetExchange("/api/copilot/conversations")
   fun getConversationHistory(): List<ConversationSummary>
}

data class UserInteractionToolResponseSubmission(
   val responses: List<UserInteractionToolResponse>
)
data class UserInteractionToolResponse(
   val toolCallId: ToolResponseCallId,
   val toolName: String,
   val toolData: Map<String, Any>
)

typealias ToolResponseCallId = String
typealias ToolResponseData = String
