package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.cockpit.core.monitoring.MetricsWindow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

/**
 * This service exposes invocation counts of services, which is used for a licensing perspective.
 */
@RestController
class OperationCountService(
   private val writer: OperationInvocationCountWriter,
   private val repository: OperationInvocationCountRepository
) {

   @GetMapping("/api/operations/counts")
   fun getOperationCounts(
      @RequestParam(name = "period", required = false, defaultValue = "MonthToDate") period: MetricsWindow,
      now: ZonedDateTime = ZonedDateTime.now()
   ): Map<String, OperationInvocationCountWindow> {
      val dateRange = period.asRange(now)

      val records = repository.getAllByWindowStartBetween(dateRange.start, dateRange.endInclusive)
      return records.groupBy { it.operationName }
         .mapValues { (_, records) ->
            records.reduce { acc, operationInvocationCountWindow ->
               acc.merge(operationInvocationCountWindow)
            }.copy(windowStart = dateRange.start, windowEnd = dateRange.endInclusive)
         }

   }
}
