package io.vyne.cask.services

import io.vyne.query.queryBuilders.VyneQlGrammar
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import org.springframework.stereotype.Service

@Service
class DefaultCaskTypeProvider {
   private val taxiDocument = mapOf("vyne.cask.types" to
      // Note: Currently including VyneQl type definitions in the cask schema.
      // This ultimately needs to move, but not sure where to yet.
      Compiler("""
         ${VyneQlGrammar.QUERY_TYPE_TAXI}
         namespace vyne.cask {
            type CaskInsertedAt inherits Instant
         }
      """.trimIndent()).compile())

   fun defaultCaskTaxiTypes(): Map<String, TaxiDocument> = taxiDocument
   fun vyneQlQueryType() = taxiDocument.values.first().type(VyneQlGrammar.QUERY_TYPE_NAME)
   fun insertedAtType() = taxiDocument.values.first().type("vyne.cask.CaskInsertedAt")
}
