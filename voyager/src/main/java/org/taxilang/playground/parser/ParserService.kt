package org.taxilang.playground.parser

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.query.VyneQlGrammar
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
      val buildInTypes = listOf(
         SourcePackage(
            PackageMetadata.from("org.taxilang", "taxiql", "0.1.0"),
            listOf(
               VersionedSource(
                  "TaxiQL",
                  version = "0.1.0",
                  VyneQlGrammar.QUERY_TYPE_TAXI
               )
            ),
            additionalSources = emptyMap()
         )

      )

      val packages = listOf(
         SourcePackage(
            PackageMetadata.from("unknown", "unknown", "1.0.0"),
            listOf(
               VersionedSource.sourceOnly(source)
            ),
            additionalSources = emptyMap()
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
