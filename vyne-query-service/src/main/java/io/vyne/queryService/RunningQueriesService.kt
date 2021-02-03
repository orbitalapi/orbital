package io.vyne.queryService

import io.vyne.RunningQueryStatus
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstanceConverter
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class RunningQueriesService(
   private val activeQueryRepository: ExecutingQueryRepository,
   private val stompTemplate: SimpMessageSendingOperations
) {
   init {
      activeQueryRepository.statusUpdates.subscribe { queryStatus ->
         stompTemplate.convertAndSend("/topic/runningQueryUpdates", queryStatus)
         stompTemplate.convertAndSend("/topic/runningQueryUpdates/${queryStatus.queryId}", queryStatus)
      }
   }

   @DeleteMapping("/api/query/active/{queryId}")
   fun stopQuery(@PathVariable("queryId") queryId: String) {
      this.activeQueryRepository.stop(queryId)
   }

   @GetMapping("/api/query/active/{queryId}")
   fun getActiveQuery(queryId: String): RunningQueryStatus {
      return activeQueryRepository.get(queryId).currentStatus()
   }

   @GetMapping("/api/query/active")
   fun listActiveQueries(): List<RunningQueryStatus> {
      return activeQueryRepository.list()
         .map { it.currentStatus() }
   }
}

@Controller
class RunningQueriesResultService(
   private val activeQueryRepository: ExecutingQueryRepository,
   private val stompTemplate: SimpMessageSendingOperations
) {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   @MessageMapping("/query/{queryId}/{responseTopicId}")
   fun subscribeForResults(
      @DestinationVariable("queryId") queryId: String,
      @DestinationVariable("responseTopicId") responseTopicId: String
   ) {
      val query = activeQueryRepository.get(queryId)
      query.resultStream()
         .subscribe { typedInstance ->
            val lightweightInstance = converter.convert(typedInstance)
            if (lightweightInstance != null) {
               stompTemplate.convertAndSend("/topic/query/$queryId/$responseTopicId", lightweightInstance)
            }

         }
   }
}
