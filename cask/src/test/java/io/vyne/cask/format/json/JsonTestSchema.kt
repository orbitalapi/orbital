package io.vyne.cask.format.json

import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema

object CoinbaseJsonOrderSchema {
   val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by jsonPath("/Symbol")
    open : Price by jsonPath("/Open")
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by jsonPath("/High")
    orderDate: Date by jsonPath("/Date")
}
type OrderWindowSummaryCsv {
    orderDate : Date by column(1)
    symbol : Symbol by column(2)
    open : Price by column(3)
    close : Price by column(4)
}
type OrderWindowSummaryXml {
    orderDate : Date by xpath("/Order/Date")
    symbol : Symbol by xpath("/Order/Symbol")
    open : Price by xpath("/Order/Open")
    close : Price by xpath("/Order/Close")
}
type OrderWindowSummaryWithPrimaryKey {
    symbol : Symbol by jsonPath("/Symbol")
    open : Price by jsonPath("/Open")
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by jsonPath("/High")
    orderDate: Date by jsonPath("/Date")
}
""".trimIndent()

   fun String.asSourcePackage(
      organisation: String = "com.coinbase",
      name: String = "test",
      version: String = "0.1.0"
   ): SourcePackage {
      return SourcePackage(
         PackageMetadata.from(organisation, name, version),
         listOf(VersionedSource.sourceOnly(this))
      )
   }

   val sourcePackageV1 = sourceV1.asSourcePackage()
   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")

   val sourceV2 = """
      type alias Price as Decimal
      type alias Symbol as String
      type alias OrderCount as Int
      type OrderWindowSummary {
          symbol : Symbol by jsonPath("/Symbol")
          open : Price by jsonPath("/Open")
          // Note, this is intentionally wrong, so we can redefine it in v2
          close : Price by jsonPath("/High")
          orderDate: Date by jsonPath("/Date")
          orderCount : OrderCount by jsonPath("/orderCount")
      }
   """.trimIndent()
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
   val sourcePackageV2 = sourceV2.asSourcePackage(version = "0.2.0")

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
   val sourcePackageV3 = sourceV3.asSourcePackage(version = "0.3.0")

   val nullableSourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol? by jsonPath("/Symbol")
    open : Price? by jsonPath("/Open")
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price? by jsonPath("/High")
    orderDate: Date? by jsonPath("/Date")
}
type OrderWindowSummaryCsv {
    orderDate : Date? by column(1)
    symbol : Symbol? by column(2)
    open : Price? by column(3)
    close : Price? by column(4)
}
""".trimIndent()
   val nullableSourceV1Package = nullableSourceV1.asSourcePackage()

   val CsvWithDefault = """
      type alias Price as Decimal
      type alias Symbol as String
      type OrderWindowSummaryCsv {
          orderDate : Date by column(1)
          symbol : Symbol by column(2)
          open : Price by column(3)
          close : Price by column(4)
          foo: String by default("")
      }
   """.trimIndent()
   val nullableSchemaV1 = TaxiSchema.from(nullableSourceV1, "Coinbase", "0.1.0")
   val observableCoinbaseWithPk = """
      type Price inherits Decimal
      type Symbol inherits String
      @ObserveChanges(writeToConnectionName = "OrderWindowSummary")
      model OrderWindowSummaryCsv {
         orderDate : Date(@format = "dd/MM/yyyy") by column(1)
         @PrimaryKey
         symbol : Symbol by column(2)
         open : Price by column(3)
         close : Price by column(4)
      }
   """.trimIndent()

   val observableCoinbase = """
      type Price inherits Decimal
      type Symbol inherits String
      @ObserveChanges(writeToConnectionName = "OrderWindowSummary")
      model OrderWindowSummaryCsv {
         orderDate : Date(@format = "dd/MM/yyyy") by column(1)
         symbol : Symbol by column(2)
         open : Price by column(3)
         close : Price by column(4)
      }
   """.trimIndent()


}
