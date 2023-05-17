package org.taxilang.playground.parser

import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationMessage
import lang.taxi.errors
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ParserService {

   @PostMapping("/api/schema/parse")
   fun parseToSchema(@RequestBody source: String): ParsedSchema {
      val (messages, schema) = TaxiSchema.compiled(
         listOf(
            SourcePackage(
               PackageMetadata.from("unknown", "unknown", "1.0.0"),
               listOf(
                  VersionedSource.sourceOnly(source)
               )
            )
         )
      )
      return ParsedSchema(schema, messages)
   }
}

data class ParsedSchema(
   val schema: TaxiSchema,
   val messages: List<CompilationMessage>
) {
   val hasErrors = messages.errors().isNotEmpty()
}
