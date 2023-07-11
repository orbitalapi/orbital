package io.vyne.cask.ingest

import io.vyne.cask.observers.KafkaObserverConfiguration
import io.vyne.schemas.taxi.TaxiSchema

object TestSchema {
   val timeTypeTest = """
type TimeTest {
   entry: String by column(1)
   time: Time by column(2)
}""".trimIndent()

   val upsertTest = """
type UpsertTestNoPk {
   id: Int by column(1)
   name: String by column(2)
   @Format("yyyy-MM-dd HH:mm:ss")
   t1: DateTime by column(3)
   v1: Decimal by column(4)
   }
type UpsertTestSinglePk {
   @PrimaryKey
   id: Int by column(1)
   name: String by column(2)
   @Format("yyyy-MM-dd HH:mm:ss")
   t1: DateTime  by column(3)
   v1: Decimal by column(4)
   }
type UpsertTestMultiPk {
   @PrimaryKey
   @Indexed
   id: Int by column(1)
   @PrimaryKey
   name: String by column(2)
   @Format("yyyy-MM-dd HH:mm:ss")
   t1: DateTime by column(3)
   v1: Decimal by column(4)
}""".trimIndent()

   val observableUpsertTestSchemaDataSourceConfigurations = listOf(
      KafkaObserverConfiguration(connectionName = "UpsertTestNoPk", bootstrapServers = "localhost:9092", topic = "UpsertTestNoPk"),
      KafkaObserverConfiguration(connectionName = "UpsertTestSinglePk", bootstrapServers = "localhost:9092", topic = "UpsertTestSinglePk"),
      KafkaObserverConfiguration(connectionName = "UpsertTestMultiPk", bootstrapServers = "localhost:9092", topic = "UpsertTestMultiPk")
   )

   val observableUpsertTestSchemaSource = """
@ObserveChanges(writeToConnectionName = "UpsertTestNoPk")
model UpsertTestNoPk {
   id: Int by column(1)
   name: String by column(2)
   @Format( "yyyy-MM-dd HH:mm:ss")
   t1: DateTime by column(3)
   v1: Decimal by column(4)
   }

@ObserveChanges(writeToConnectionName = "UpsertTestSinglePk")
model UpsertTestSinglePk {
   @PrimaryKey
   id: Int by column(1)
   name: String by column(2)
   @Format( "yyyy-MM-dd HH:mm:ss")
   t1: DateTime by column(3)
   v1: Decimal by column(4)
   }

@ObserveChanges(writeToConnectionName = "UpsertTestMultiPk")
model UpsertTestMultiPk {
   @PrimaryKey
   @Indexed
   id: Int by column(1)
   @PrimaryKey
   name: String by column(2)
   @Format( "yyyy-MM-dd HH:mm:ss")
   t1: DateTime by column(3)
   v1: Decimal by column(4)
}""".trimIndent()



   val temporalSchemaSource = """
      type DowncastTest {
         @Format( "yyyy-MM-dd'T'HH:mm:ss")
         dateOnly: Date by column(1)
         @Format("yyyy-MM-dd'T'HH:mm:ss")
         timeOnly: Time by column(2)
      }
   """.trimIndent()

   val schemaWithDefaultValueSource = """
      model ModelWithDefaults {
         field1: String by column("FIRST_COLUMN")
         defaultString: String ?: "Default String"
         defaultDecimal: Decimal ?: 1000000.0
      }

   """.trimIndent()

   val schemaConcatSource = """
      model ConcatModel {
         concatField: String by concat(column("FIRST_COLUMN"), "-", column("SECOND_COLUMN"), "-", column("THIRD_COLUMN"))
      }

   """.trimIndent()

   val instantFormatSource = """
      model InstantModel {
      @Format ( "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        instant: Instant? by column("ValidityPeriodDateAndTime")
      }
   """.trimIndent()


   val schemaWithConcatAndDefaultSource = """
      model ModelWithDefaultsConcat {
         field1: String by column("FIRST_COLUMN")
         defaultString: String ?: "Default String"
         defaultDecimal: Decimal ?: 1000000.0
         concatField: String by concat(fcolumn("FIRST_COLUMN"), "-", column("SECOND_COLUMN"), "-", column("THIRD_COLUMN"))
      }
   """.trimIndent()

   val personSchemaSource = """
      type PersonId inherits Int
      type FirstName inherits String
      type LastName inherits String

      type Person {
         id: PersonId by column("Id")
         firstName: FirstName by column("FirstName")
         lastName: LastName by column("LastName")
      }
   """.trimIndent()

   val decimalSchemaSource = """
      model DecimalModel {
        qty: Decimal by column("Quantity")
      }
      """.trimIndent()

   val enumConcatSchemaSource = """
      enum Leg1BankPayReceive {
         Pay,
         Receive
      }

      enum Leg2BankPayReceive {
         Pay,
         Receive
      }

      enum IcapLeg2PayReceive {
      	Receive("BUYI") synonym of Leg2BankPayReceive.Receive,
      	Pay("SELL") synonym of Leg2BankPayReceive.Pay
      }

      enum IcapLeg1PayReceive {
      	Pay("BUYI") synonym of Leg1BankPayReceive.Pay,
      	Receive("SELL") synonym of Leg1BankPayReceive.Receive
      }

      model OrderI {
         tempPayReceive: String? by concat(column("InstrumentClassification"),"-",column("BuySellIndicator"))
         leg1PayReceive: IcapLeg1PayReceive? by when(this.tempPayReceive) {
		      "SRCCSP-BUYI" -> Leg1BankPayReceive.Pay
		      "SRCCSP-SELL" -> Leg1BankPayReceive.Receive
            else -> null
         }

         leg2PayReceive: IcapLeg2PayReceive? by when(this.tempPayReceive) {
		      "SRCCSP-BUYI" -> Leg2BankPayReceive.Receive
		      "SRCCSP-SELL" -> Leg2BankPayReceive.Pay
            else -> null
	      }

         tempLegRate: String? by concat(column("InstrumentClassification"),"-",column("BuySellIndicator"))
            leg1Rate: Leg1Rate? by when (this.tempLegRate) {
               "SRCCSP-BUYI" -> column ("LimitPrice")
               else -> null
             }
      }
   """.trimIndent()

   val schemaTimeTest = TaxiSchema.from(timeTypeTest, "Test", "0.1.0")
   val schemaUpsertTest = TaxiSchema.from(upsertTest, "Test", "0.1.0")
   val schemaObservableUpsertTestSchema = TaxiSchema.from(observableUpsertTestSchemaSource, "Test", "0.1.0")
   val schemaTemporalDownCastTest = TaxiSchema.from(temporalSchemaSource, "Test", "0.1.0")
   val schemaWithDefault = TaxiSchema.from(schemaWithDefaultValueSource, "Test", "0.1.0")
   val schemaConcat = TaxiSchema.from(schemaConcatSource, "test", "0.1.0")
   val instantSchema = TaxiSchema.from(instantFormatSource, "test", "0.1.0")
   val schemaWithConcatAndDefault = TaxiSchema.from(schemaWithDefaultValueSource, "test", "0.1.0")
   val personSchema = TaxiSchema.from(personSchemaSource, "test", "0.1.0")
   val decimalSchema = TaxiSchema.from(decimalSchemaSource, "test", "0.1.0")
}


