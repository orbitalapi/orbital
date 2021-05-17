package io.vyne.dataQuality.api

import io.vyne.dataQuality.DataSubjectQualityReportEvent
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient("data-quality-hub")
interface DataQualityEventServiceApi {
   @PostMapping("/events")
   fun submitQualityReportEvent(
      @RequestBody event: DataSubjectQualityReportEvent
   ): ResponseEntity<String>
}
