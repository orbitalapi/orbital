package io.vyne.queryService.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.queryService.WebSocketController
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
@RestController
@EnableScheduling
class SchemaChangeNotificationService(private val mapper: ObjectMapper) : WebSocketController {

   private val schemaUpdatedEventSink = MutableSharedFlow<SchemaUpdatedNotification>()
   val schemaUpdatedNotificationEvents = schemaUpdatedEventSink.asSharedFlow()

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Received schema set changed event, sending UI notification")
      notifySchemaUpdatedNotification(
         SchemaUpdatedNotification(
            event.newSchemaSet.id,
            event.newSchemaSet.generation,
            event.newSchemaSet.invalidSources.size
         )
      )
   }

   fun notifySchemaUpdatedNotification(notification: SchemaUpdatedNotification) = runBlocking { // this: CoroutineScope
      launch { // launch a new coroutine in the scope of runBlocking
         schemaUpdatedEventSink.emit(notification)
      }
   }

   override val paths: List<String> = listOf("/api/schema/updates")
   override fun handle(session: WebSocketSession): Mono<Void> {
      return session.send(
         schemaUpdatedNotificationEvents
            .map { mapper.writeValueAsString(it) }
            .map(session::textMessage)
            .asFlux())
   }
}

/**
 * Lighweight notification sent to the UI when the
 * schemaset changes
 */
data class SchemaUpdatedNotification(
   val newId: Int,
   val generation: Int,
   val invalidSourceCount: Int
)
