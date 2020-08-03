package io.vyne.schemaStore

import io.vyne.VersionedSource
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@FeignClient("\${vyne.queryService.name:query-service}")
interface HttpVersionedSchemaProvider {
   @RequestMapping(method = [RequestMethod.GET], value = ["/api/schemas"])
   fun getVersionedSchemas(): List<VersionedSource>
}
