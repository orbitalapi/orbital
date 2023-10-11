package com.orbitalhq.query.runtime.core.monitor

import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ActiveQueryController(private val monitor: ActiveQueryMonitor) {
   @GetMapping("/api/query/active")
   fun liveQueries(): Map<String, RunningQueryStatus> {
      return monitor.runningQueries()
   }

   @DeleteMapping("/api/query/active/{id}")
   @PreAuthorize("hasAuthority('${VynePrivileges.CancelQuery}')")
   fun cancelQuery(
      @PathVariable("id") queryId: String
   ) {
      if (!monitor.cancelQuery(queryId)) {
         throw NotFoundException("No query with id $queryId was found")
      }
   }

   @DeleteMapping("/api/query/active/clientId/{id}")
   @PreAuthorize("hasAuthority('${VynePrivileges.CancelQuery}')")
   fun cancelQueryByClientQueryId(
      @PathVariable("id") clientQueryId: String
   ) {
      if (!monitor.cancelQueryByClientQueryId(clientQueryId)) {
         throw NotFoundException("No query with clientQueryID $clientQueryId was found")
      }
   }

}



