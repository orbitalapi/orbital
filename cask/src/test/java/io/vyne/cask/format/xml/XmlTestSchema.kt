package io.vyne.cask.format.xml

import io.vyne.schemas.taxi.TaxiSchema

object XmlTestSchema {
   val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummaryXml {
    orderDate : Date by xpath("/Order/Date")
    symbol : Symbol by xpath("/Order/Symbol")
    open : Price by xpath("/Order/Open")
    close : Price by xpath("/Order/Close")
    volume: Decimal? by xpath("/Order/Volume")
}
""".trimIndent()
   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
}
