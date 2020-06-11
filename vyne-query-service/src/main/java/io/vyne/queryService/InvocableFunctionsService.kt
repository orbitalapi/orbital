package io.vyne.queryService
import io.vyne.schemas.Type
import io.vyne.spring.VyneFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InvocableFunctionsService(val vyneFactory: VyneFactory) {
   @GetMapping("/accessible_from/{fqn}")
   fun invocableFunctionsForType(@PathVariable("fqn") fqn: String): Set<Type> {
      val vyne = vyneFactory.createVyne()
      return vyne.accessibleFrom(fqn)
   }
}

