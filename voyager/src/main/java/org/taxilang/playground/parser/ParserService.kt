package org.taxilang.playground.parser

import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.query.VyneQlGrammar
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
      val buildInTypes = listOf(
         SourcePackage(
            PackageMetadata.from("org.taxilang", "taxiql", "0.1.0"),
            listOf(
               VersionedSource(
                  "TaxiQL",
                  version = "0.1.0",
                  VyneQlGrammar.QUERY_TYPE_TAXI
               )
            )
         )

      )

      val packages = listOf(
         SourcePackage(
            PackageMetadata.from("unknown", "unknown", "1.0.0"),
            listOf(
               VersionedSource.sourceOnly(source)
            )
         )
      ) + buildInTypes
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
