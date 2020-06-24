package io.vyne.queryService

import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.spring.VyneFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InvocableFunctionsService(val vyneFactory: VyneFactory) {
   @GetMapping("/api/types/{qualifiedName}/discoverable-types")
   fun invocableFunctionsForType(@PathVariable("qualifiedName") fqn: String): List<QualifiedName> {
      val vyne = vyneFactory.createVyne()
      return vyne.accessibleFrom(fqn).map { it.qualifiedName }
   }
}

