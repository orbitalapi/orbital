package io.vyne.queryService.schemas

import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import io.vyne.schemaStore.SchemaProvider
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FileToTypeParserService(val schemaProvider: SchemaProvider) {

   @PostMapping("/content/parse")
   fun parseFileToType(@RequestBody rawContent: String, @RequestParam("type") typeName: String): ParsedTypeInstance {
      val schema = schemaProvider.schema()
      val targetType = schema.type(typeName)
      return ParsedTypeInstance(TypedInstance.from(targetType, rawContent, schema))
   }
}

data class ParsedTypeInstance(
   val instance: TypedInstance
) {
   val typeNamedInstance = instance.toTypeNamedInstance()
   val raw = instance.toRawObject()
}
