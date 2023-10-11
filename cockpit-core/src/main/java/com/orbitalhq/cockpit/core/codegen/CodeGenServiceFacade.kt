package com.orbitalhq.cockpit.core.codegen

import com.orbitalhq.schemaServer.codegen.CodeGenApi
import com.orbitalhq.spring.config.ExcludeFromOrbitalStation
import com.orbitalhq.spring.http.handleFeignErrors
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
