package io.vyne.cask.format.json

import io.vyne.schemas.taxi.TaxiSchema

object CoinbaseJsonOrderSchema {
   val sourceV1 = """
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
    orderDate : Date by column(1)
    symbol : Symbol by column(2)
    open : Price by column(3)
    close : Price by column(4)
}
""".trimIndent()
   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")

   val sourceV2 = """
      type alias Price as Decimal
      type alias Symbol as String
      type alias OrderCount as Int
      type OrderWindowSummary {
          symbol : Symbol by xpath("/Symbol")
          open : Price by xpath("/Open")
          // Note, this is intentionally wrong, so we can redefine it in v2
          close : Price by xpath("/High")
          orderDate: Date by xpath("/Date")
          orderCount : OrderCount by xpath("/orderCount")
      }
   """.trimIndent()
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")

   val sourceV3 = """
      type alias Price as Decimal
      type alias Symbol as String
      type alias OrderCount as Int
      type OrderWindowSummary {
          symbol : Symbol by xpath("/Symbol")
          open : Price by xpath("/Open")
          close : Price by xpath("/Close")
          orderDate: Date by xpath("/Date")
          orderCount : OrderCount by xpath("/orderCount")
      }
   """.trimIndent()
   val schemaV3 = TaxiSchema.from(sourceV3, "Coinbase", "0.3.0")
}
