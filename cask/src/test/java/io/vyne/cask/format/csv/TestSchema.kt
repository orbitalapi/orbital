package io.vyne.cask.format.csv

import io.vyne.schemas.taxi.TaxiSchema

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

   private val timeTypeTest = """
type TimeTest {
   entry: String by column(1)
   time: Time by column(2)
}""".trimIndent()

   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
   val schemaV3 = TaxiSchema.from(sourceV3, "Coinbase", "0.3.0")
   val schemaTimeTest = TaxiSchema.from(timeTypeTest, "Test", "0.1.0")
}
