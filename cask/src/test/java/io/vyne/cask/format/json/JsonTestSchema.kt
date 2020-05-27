package io.vyne.cask.format.json

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
    orderDate: Date by xpath("/Date")
}
type OrderWindowSummaryCsv {
    orderDate : Date by column(0)
    symbol : Symbol by column(1)
    open : Price by column(2)
    close : Price by column(3)
}
""".trimIndent()
   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
}
