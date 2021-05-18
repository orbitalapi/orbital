package io.vyne.dataQuality.api

import io.vyne.dataQuality.DataSubjectQualityReportEvent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("data-quality-hub")
interface DataQualityEventServiceApi {
   @PostMapping("/api/events")
   fun submitQualityReportEvent(
      @RequestBody event: DataSubjectQualityReportEvent
   ): Mono<String>
}
