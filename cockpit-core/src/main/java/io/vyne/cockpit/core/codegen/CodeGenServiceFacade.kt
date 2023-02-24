package io.vyne.cockpit.core.codegen

import io.vyne.schemaServer.codegen.CodeGenApi
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@ExcludeFromOrbitalStation
class CodeGenServiceFacade(private val codeGenApi: CodeGenApi) {

   @GetMapping("/api/taxonomy/typescript", "/api/codegen/typescript")
   fun getTypeScriptTaxonomy(): Mono<String> = handleFeignErrors {
      codeGenApi.getTypeScriptTaxonomy()
   }
}
