package io.vyne.schemaServer.taxonomy

import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server}", qualifier = "schemaTaxonomyFeignClient")
interface TaxonomyApi {
   @GetMapping("/api/taxonomy/typescript")
   fun getTypeScriptTaxonomy(): Mono<String>
}
