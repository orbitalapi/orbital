package org.taxilang.playground.parser

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.CompilationMessage
import lang.taxi.errors
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ParserService {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
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
      val (messages, schema) = try {
         TaxiSchema.compiled(packages)
      } catch (e:Exception) {
         val message = "Taxi parser exception ocuurred - ${e.message} - the failing source follows\n${source}"
         throw TaxiParsingException(message, e)
      }
      return ParsedSchema(schema, messages)
   }
}

data class ParsedSchema(
   val schema: TaxiSchema,
   val messages: List<CompilationMessage>
) {
   val hasErrors = messages.errors().isNotEmpty()
}

class TaxiParsingException(message: String, cause:Throwable) : RuntimeException(message, cause)
