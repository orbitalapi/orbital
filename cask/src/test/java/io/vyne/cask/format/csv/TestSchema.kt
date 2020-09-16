package io.vyne.cask.format.csv

import io.vyne.schemas.taxi.TaxiSchema

object CoinbaseOrderSchema {
   val sourceV1 = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(2)
    open : Price by column(3)
    // Note, this is intentionally wrong, so we can redefine it in v2
    close : Price by column(4)
}""".trimIndent()

   val sourceV2 = """
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

   val personSourceV1 = """
      namespace demo {
         type PersonId inherits String
         type FirstName inherits String
         type LastName inherits String

         type Person {
            id : PersonId by column("id")

            firstName : FirstName by column("firstName")

      	  lastName : LastName by column("lastName")
         }
      }
   """.trimIndent()
   val personSourceV2 = """
namespace demo {
   type PersonId inherits String
   type FirstName inherits String
   type LastName inherits String
   type LogDate inherits Date
   type LogTime inherits Time

   type Person {
      @Id
	  @PrimaryKey
      id : PersonId by column("id")

	  @Association
	  @Indexed
      firstName : FirstName by column("firstName")

	  lastName : LastName by column("lastName")

	  @Between
	  logDate : LogDate( @format ="dd/MMM/yyyy") by column("logDate")

	  logTime: LogTime( @format = "HH:mm:ss.SSS") by column("logTime")
   }
}""".trimIndent()

   val schemaV1 = TaxiSchema.from(sourceV1, "Coinbase", "0.1.0")
   val schemaV2 = TaxiSchema.from(sourceV2, "Coinbase", "0.2.0")
   val schemaV3 = TaxiSchema.from(sourceV3, "Coinbase", "0.3.0")
   val personSchemaV1 = TaxiSchema.from(personSourceV1, "Coinbase", "0.3.0")
   val personSchemaV2 = TaxiSchema.from(personSourceV2, "Coinbase", "0.3.0")
}
