package io.vyne.cask.format.json

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.cask.format.csv.TestSchemaStoreClient
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.taxi.TaxiSchema

object CoinbaseJsonOrderSchema {
   private val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by xpath("/High")
}""".trimIndent()

   private val sourceV2 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    // Added column
    high : Price by xpath("/High")
    // Changed column
    close : Price by xpath("/Close")
}""".trimIndent()

   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
   val schemaStoreClient = TestSchemaStoreClient(SchemaSet.fromParsed(listOf(ParsedSource(VersionedSource("CoinbaseJson", "0.1.0", sourceV1))), 0))
}
