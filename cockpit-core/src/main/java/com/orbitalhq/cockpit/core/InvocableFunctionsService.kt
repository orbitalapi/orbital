package com.orbitalhq.cockpit.core

import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.VyneFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InvocableFunctionsService(val vyneFactory: VyneFactory) {
   @PreAuthorize("hasAuthority('${VynePrivileges.BrowseSchema}')")
   @GetMapping("/api/types/{qualifiedName}/discoverable-types")
   suspend fun invocableFunctionsForType(@PathVariable("qualifiedName") fqn: String): List<QualifiedName> {
      val vyne = vyneFactory.createVyne()
      return vyne.accessibleFrom(fqn).map { it.qualifiedName }
   }
}

