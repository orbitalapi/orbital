package com.orbitalhq.copilot

import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.security.VynePrivileges
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
class CopilotQueryService(private val parser: OpenAiChatService, private val schemaProvider: SchemaProvider) {

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping("/api/copilot/conversation/query")
   fun parseChatQuery(@RequestBody messages: List<ConversationMessage>): Mono<ConversationMessage> {
      return Mono.fromCallable {
         val schema = schemaProvider.schema
         parser.submitConversationFromMessages(schema, messages)
      }.subscribeOn(Schedulers.boundedElastic())
   }
}

