package com.orbitalhq.nebula

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.nebula.core.NebulaStackState
import com.orbitalhq.spring.http.websocket.OrbitalWebSocketConfiguration
import com.orbitalhq.spring.http.websocket.WebSocketController
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import reactor.core.publisher.Mono

@RestController
class NebulaService(
   private val submissionService: NebulaSubmissionService,
   private val mapper: ObjectMapper,
   private val orbitalWebSocketConfiguration: OrbitalWebSocketConfiguration
) : WebSocketController {

   private var lastNebulaStateUpdatedEvent: NebulaStateUpdatedEvent = NebulaStateUpdatedEvent.empty()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      submissionService.stateUpdates
         .subscribe { event ->
            this.lastNebulaStateUpdatedEvent = event
         }
   }


   @GetMapping("/api/stubs")
   fun getNebulaStack(): NebulaStateUpdatedEvent {
      return lastNebulaStateUpdatedEvent
   }

   override val paths: List<String> = listOf("/api/stubs/updates")

   override fun handle(session: WebSocketSession): Mono<Void> {
      val outbound = submissionService.stateUpdates
         .map { mapper.writeValueAsString(it) }
         .map(session::textMessage)
      return orbitalWebSocketConfiguration.applyPingConfiguration(
         this, session, outbound
      )
   }


}

/**
 * Note that the nebula entry here should be resolved via services.conf
 */
@HttpExchange("http://nebula")
interface NebulaApi {
   /**
    * Returns the names of current stacks
    */
   @GetExchange("/stacks")
   fun getStacks(): Mono<NebulaStackState>

   /**
    * Creates a new stack.
    * Returns the newly created stack name
    */
   @PostExchange("/stacks")
   fun postStack(@RequestBody scriptSource: String): Mono<String>

   /**
    * Updates an existing stack.
    * Returns the updated stacks name
    */
   @PutExchange("/stacks/{name}")
   fun updateStack(@PathVariable("name") name: String, @RequestBody script: String): Mono<String>

   /**
    * Deletes a stack.
    * Returns the list of remaining stack names.
    */
   @DeleteExchange("/stacks/{name}")
   fun deleteStack(@PathVariable("name") name: String): Mono<List<String>>
}

// copy of Nebula's Component info
data class ComponentInfo(
   val container: ContainerInfo?,
   val componentConfig: Map<String, Any>
)

data class ContainerInfo(
   val containerId: String,
   val imageName: String,
   val containerName: String,
)

data class UpdateStackRSocketRequest(val stacks: Map<String, String>)
