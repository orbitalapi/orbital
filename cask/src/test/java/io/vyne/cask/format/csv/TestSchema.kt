package io.vyne.cask.format.csv

import arrow.core.Either
import arrow.core.right
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemaStore.SchemaValidator
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationException

object CoinbaseOrderSchema {
   private val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(2)
    open : Price by column(3)
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by column(4)
}""".trimIndent()

   private val sourceV2 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(2)
    open : Price by column(3)
    // Added column
    high : Price by column(4)
    // Changed column
    close : Price by column(6)
}""".trimIndent()

   private val sourceV3 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column("Symbol")
    open : Price by column("Open")
    // Added column
    high : Price by column("High")
    // Changed column
    close : Price by column("Close")
}""".trimIndent()

   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
   val schemaV3 = TaxiSchema.from(sourceV3, "Coinbase", "0.2.0")
   val schemaStoreClient = TestSchemaStoreClient(SchemaSet.fromParsed(listOf(ParsedSource(VersionedSource("Coinbase", "0.1.0", sourceV1))), 0))
}

class TestSchemaStoreClient(val schemaSet: SchemaSet, val validator: SchemaValidator = TaxiSchemaValidator()): SchemaStoreClient {
   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      val validationResult = validator.validate(schemaSet, versionedSources)
      val (parsedSources,returnValue)  = when(validationResult) {
         is Either.Right -> {
            validationResult.b.second to Either.right(validationResult.b.first)
         }
         is Either.Left -> {
            validationResult.a.second to Either.left(validationResult.a.first)
         }
      }
      return returnValue
   }

   override fun schemaSet(): SchemaSet {
      TODO("Not yet implemented")
   }

   override val generation: Int
      get() = TODO("Not yet implemented")

}
