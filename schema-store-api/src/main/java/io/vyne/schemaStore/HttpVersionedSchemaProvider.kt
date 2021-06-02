package io.vyne.schemaStore

import io.vyne.VersionedSource
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.queryService.name:query-service}")
interface HttpVersionedSchemaProvider {
   @RequestMapping(method = [RequestMethod.GET], value = ["/api/schemas"])
   fun getVersionedSchemas(): Mono<List<VersionedSource>>
}
