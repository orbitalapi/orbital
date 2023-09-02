package com.orbitalhq.cockpit.core.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.SchemaSetChangedEvent
import com.orbitalhq.spring.http.websocket.WebSocketController
import com.orbitalhq.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@RestController
@EnableScheduling
class SchemaChangeNotificationService(
   private val mapper: ObjectMapper,
   private val schemaStore: SchemaStore
) : WebSocketController, InitializingBean {

   private val schemaUpdatedEventSink = MutableSharedFlow<SchemaUpdatedNotification>()
   val schemaUpdatedNotificationEvents = schemaUpdatedEventSink.asSharedFlow()

   override fun afterPropertiesSet() {
      Flux.from(schemaStore.schemaChanged).subscribe { onSchemaSetChanged(it) }
   }

   private fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Received schema set changed event, sending UI notification")
      notifySchemaUpdatedNotification(
         SchemaUpdatedNotification(
            event.newSchemaSet.id,
            event.newSchemaSet.generation,
            event.newSchemaSet.sourcesWithErrors.size
         )
      )
   }

   fun notifySchemaUpdatedNotification(notification: SchemaUpdatedNotification) =
      GlobalScope.launch { // this: CoroutineScope
         schemaUpdatedEventSink.emit(notification)
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
 * Lightweight notification sent to the UI when the
 * schemaset changes
 */
data class SchemaUpdatedNotification(
   val newId: Int,
   val generation: Int,
   val invalidSourceCount: Int
)
