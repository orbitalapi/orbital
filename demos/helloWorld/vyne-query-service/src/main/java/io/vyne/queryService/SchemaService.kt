package io.vyne.queryService

import io.osmosis.polymer.schemas.Schema
import io.polymer.schemaStore.SchemaSourceProvider
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class SchemaService(private val schemaProvider: SchemaSourceProvider) {
   @RequestMapping(path = arrayOf("/schemas"), method = arrayOf(RequestMethod.GET))
   fun listRawSchema():String {
      return schemaProvider.schemaStrings().joinToString("\n")
   }

   @RequestMapping(path = arrayOf("/types"), method = arrayOf(RequestMethod.GET))
   fun getTypes(): Schema {
      return schemaProvider.schema()
   }
}
