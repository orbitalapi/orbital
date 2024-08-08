package org.taxilang.playground.parser

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.CompilationMessage
import lang.taxi.errors
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ParserService {
   @PostMapping("/api/schema/parse")
   fun parseToSchema(@RequestBody source: String): ParsedSchema {
      val packages = listOf(
         SourcePackage(
            PackageMetadata.from("unknown", "unknown", "1.0.0"),
            listOf(
               VersionedSource.sourceOnly(source)
            ),
            additionalSources = emptyMap()
         )
      ) + StubQueryService.builtInTypesSourcePackage
      val (messages, schema) = TaxiSchema.compiled(packages)
      return ParsedSchema(schema, messages)
   }
}

data class ParsedSchema(
   val schema: TaxiSchema,
   val messages: List<CompilationMessage>
) {
   val hasErrors = messages.errors().isNotEmpty()
}
