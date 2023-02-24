package io.vyne.schemaServer.codegen

import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server-codegen.name:schema-server}", qualifier = "schemaTaxonomyFeignClient")
interface CodeGenApi {
   @GetMapping("/api/taxonomy/typescript")
   fun getTypeScriptTaxonomy(): Mono<String>
}
