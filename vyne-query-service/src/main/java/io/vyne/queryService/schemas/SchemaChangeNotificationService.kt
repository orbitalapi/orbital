package io.vyne.queryService.schemas

import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Component
@RestController
@EnableScheduling
class SchemaChangeNotificationService(
) {

   private val _schemaUpdatedNotificationEvents = MutableSharedFlow<SchemaUpdatedNotification>()
   val schemaUpdatedNotificationEvents = _schemaUpdatedNotificationEvents.asSharedFlow()

   @GetMapping("/api/events/schema", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   suspend fun schemaUpdateNotificationsEvents(): Flow<Any?>? {
      return schemaUpdatedNotificationEvents
   }

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Received schema set changed event, sending UI notification")
      notifySchemaUpdatedNotification(SchemaUpdatedNotification(
         event.newSchemaSet.id,
         event.newSchemaSet.generation,
         event.newSchemaSet.invalidSources.size
      ))
   }

   fun notifySchemaUpdatedNotification(notification:SchemaUpdatedNotification) = runBlocking { // this: CoroutineScope
      launch { // launch a new coroutine in the scope of runBlocking
         _schemaUpdatedNotificationEvents.emit(notification)
      }
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
