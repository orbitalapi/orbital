package io.vyne.queryService.schemas

import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController

@Component
@RestController
class SchemaChangeNotificationService(
   val stompTemplate: SimpMessagingTemplate
) {
   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Received schema set changed event, sending UI notification")
      stompTemplate.convertAndSend("/topic/schemaNotifications",
         SchemaUpdatedNotification(
            event.newSchemaSet.id,
            event.newSchemaSet.generation,
            event.newSchemaSet.invalidSources.size
         ))
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
