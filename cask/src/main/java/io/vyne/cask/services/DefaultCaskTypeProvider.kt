package io.vyne.cask.services

import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import org.springframework.stereotype.Service

@Service
class DefaultCaskTypeProvider {
   private val taxiDocument = mapOf("vyne.cask.types" to
      Compiler("""
         namespace vyne.cask {
            type CaskInsertedAt inherits Instant
         }
      """.trimIndent()).compile())

   fun defaultCaskTaxiTypes():Map<String, TaxiDocument> = taxiDocument
   fun insertedAtType() = taxiDocument.values.first().type("vyne.cask.CaskInsertedAt")
}
