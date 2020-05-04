package io.vyne.cask.format.csv

import io.vyne.schemas.taxi.TaxiSchema

object CoinbaseOrderSchema {
    private val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(1)
    open : Price by column(2)
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by column(3)
}""".trimIndent()

    private val sourceV2 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(1)
    open : Price by column(2)
    // Added column
    high : Price by column(3)
    // Changed column
    close : Price by column(5)
}""".trimIndent()

    val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
    val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
}
