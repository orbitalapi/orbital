package com.orbitalhq.copilot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.websocket.WebSocketController
import com.orbitalhq.utils.Ids
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

@RestController
class CopilotServiceFacade(
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper,
   private val copilotApi: CopilotConversationApi,
   private val copilotSettings: CopilotSettings,
) : WebSocketController {
   override val paths: List<String> = listOf("/api/copilot/session/{sessionId}/stream")
   private val uriTemplate = UriTemplate(paths.single())
   override fun handle(session: WebSocketSession): Mono<Void> {
      val uriVariables = uriTemplate.match(session.handshakeInfo.uri.path)
      val sessionId = uriVariables["sessionId"] ?: throw BadRequestException("Invalid sessionId")

      return session.receive()
         .map {
            val payload = it.payloadAsText
            objectMapper.readValue<ConversationMessage>(payload)
         }
         .flatMap { payloadJson ->
            copilotApi.submitMessageToConversationStream(payloadJson, sessionId)
               .map { serverSentEvent ->
                  val event = serverSentEvent.data()
                  session.textMessage(objectMapper.writeValueAsString(event))
               }
               .`as`(session::send)
               .onErrorResume { error ->
                  sendErrorAndClose(session, "Stream error: ${error.message}")
               }
         }
         .then()
   }

   private fun sendErrorAndClose(session: WebSocketSession, errorMessage: String): Mono<Void> {
      val errorResponse = ErrorResponse(errorMessage)
      val errorMsg = session.textMessage(objectMapper.writeValueAsString(errorResponse))
      return session.send(Mono.just(errorMsg))
         .then(session.close(CloseStatus.SERVER_ERROR))
   }

   private val webClient = WebClient.create(copilotSettings.endpointUrl)

   @GetMapping("/api/copilot/conversations")
   fun getConversationHistory(): Mono<List<ConversationSummary>> {
      return Mono.defer {
         Mono.just(copilotApi.getConversationHistory())
      }.subscribeOn(Schedulers.boundedElastic())
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping("/api/copilot/session")
   fun parseChatQuery(): Mono<ConversationSummary> {
      return Mono.defer {
         val sessionMessage = CreateConversationSessionMessage(schemaProvider.packages)
         Mono.just(copilotApi.startSession(sessionMessage))
      }.subscribeOn(Schedulers.boundedElastic())
   }

   @GetMapping("/api/copilot/session/{sessionId}")
   fun getSessionMessages(@PathVariable sessionId: String): Mono<List<ConversationMessage>> {
      return Mono.defer {
         Mono.just(copilotApi.getSessionMessages(sessionId))
      }.subscribeOn(Schedulers.boundedElastic())
   }

   @GetMapping("/api/copilot/session/{sessionId}/events")
   fun getSessionChatEvents(@PathVariable sessionId: String): Mono<List<ChatEvent>> {
      return Mono.defer {
         Mono.just(copilotApi.getSessionChatEvents(sessionId))
      }.subscribeOn(Schedulers.boundedElastic())
   }

   @PostMapping("/api/copilot/session/{sessionId}/title")
   fun renameConversation(@PathVariable sessionId: String): Mono<ConversationSummary> {
      return copilotApi.renameConversation(sessionId)
   }

   @PostMapping("/api/copilot/session/{sessionId}/messages", consumes = [MediaType.APPLICATION_JSON_VALUE])
   fun submitMessage(
      @RequestBody message: ConversationMessage,
      @PathVariable("sessionId") sessionId: String
   ): Mono<ConversationMessage> {
      return copilotApi.submitMessageToConversation(message, sessionId)
   }

   @PostMapping("/api/copilot/session/{sessionId}/messages/stream", consumes = [MediaType.APPLICATION_JSON_VALUE])
   fun submitMessageStream(
      @RequestBody message: ConversationMessage,
      @PathVariable("sessionId") sessionId: String
   ): Flux<ServerSentEvent<ChatEvent>> {
      return copilotApi.submitMessageToConversationStream(message, sessionId)
   }


   @PostMapping(
      "/api/copilot/session/{sessionId}/messages",
      consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   fun submitMessageWithFiles(
      @RequestPart("message") messageJson: String,
      @RequestPart("files") files: Flux<FilePart>,
      @PathVariable("sessionId") sessionId: String
   ): Mono<String> {
      // I tried using the standard facade api, but
      // the Flux<FilePart> was getting sent as Flux<Part>,
      // so we lost file information
      return files.collectList().flatMap { fileParts ->
         val bodyBuilder = MultipartBodyBuilder()
         bodyBuilder.part("message", messageJson)

         // Using asyncPart for reactive content streams
         for (filePart in fileParts) {
            bodyBuilder.asyncPart("files", filePart.content(), DataBuffer::class.java)
               .filename(filePart.filename())
               .contentType(filePart.headers().contentType!!)
         }

         webClient.post()
            .uri("/api/copilot/session/$sessionId/message-with-attachments", sessionId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(String::class.java)
      }
   }

//      return copilotApi.submitMessageWithAttachments(
//         messageJson, files as Flux<Part>, sessionId
//      ).map { objectMapper.readValue<ConversationMessage>(it) }


   @PostMapping("/api/copilot/session/{sessionId}/userResponse")
   fun submitUserToolResponse(
      @RequestBody toolCallResponses: UserInteractionToolResponseSubmission,
      @PathVariable("sessionId") sessionId: String
   ): Mono<ConversationMessage> {
      return Mono.defer {
         Mono.just(copilotApi.submitUserToolResponse(toolCallResponses, sessionId))
      }.subscribeOn(Schedulers.boundedElastic())
   }
}


data class ErrorResponse(
   val error: String,
   val timestamp: String = LocalDateTime.now().toString()
)
