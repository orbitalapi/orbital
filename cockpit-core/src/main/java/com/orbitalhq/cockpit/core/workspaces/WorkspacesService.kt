package com.orbitalhq.cockpit.core.workspaces

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WorkspacesService {

   @GetMapping("/api/workspaces")
   fun listWorkspaces(auth: Authentication): List<WorkspaceSummary> {
      return emptyList()
   }
}


data class WorkspaceSummary(
   val id: String,
   val name: String
)
