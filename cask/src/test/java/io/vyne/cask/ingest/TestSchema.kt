package io.vyne.cask.ingest

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
   t1: DateTime (@format = "yyyy-MM-dd HH:mm:ss") by column(3)
   v1: Decimal by column(4)
   }
type UpsertTestSinglePk {
   @PrimaryKey
   id: Int by column(1)
   name: String by column(2)
   t1: DateTime (@format = "yyyy-MM-dd HH:mm:ss") by column(3)
   v1: Decimal by column(4)
   }
type UpsertTestMultiPk {
   @PrimaryKey
   @Indexed
   id: Int by column(1)
   @PrimaryKey
   name: String by column(2)
   t1: DateTime (@format = "yyyy-MM-dd HH:mm:ss") by column(3)
   v1: Decimal by column(4)
}""".trimIndent()


   val temporalSchemaSource = """
      type DowncastTest {
         dateOnly: Date(@format = "yyyy-MM-dd'T'HH:mm:ss") by column(1)
         timeOnly: Time(@format = "yyyy-MM-dd'T'HH:mm:ss") by column(2)
      }
   """.trimIndent()

   val schemaWithDefaultValueSource = """
      model ModelWithDefaults {
         field1: String by column("FIRST_COLUMN")
         defaultString: String by default("Default String")
         defaultDecimal: Decimal by default("1000000.0")
      }

   """.trimIndent()

   val schemaConcatSource = """
      model ConcatModel {
         concatField: String by concat(column("FIRST_COLUMN"), "-", column("SECOND_COLUMN"), "-", column("THIRD_COLUMN"))
      }

   """.trimIndent()

   val instantFormatSource = """
      model InstantModel {
        instant: Instant?(@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") by column("ValidityPeriodDateAndTime")
      }
   """.trimIndent()


   val schemaWithConcatAndDefaultSource = """
      model ModelWithDefaultsConcat {
         field1: String by column("FIRST_COLUMN")
         defaultString: String by default("Default String")
         defaultDecimal: Decimal by default("1000000.0")
         concatField: String by concat(column("FIRST_COLUMN"), "-", column("SECOND_COLUMN"), "-", column("THIRD_COLUMN"))
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

   val schemaTimeTest = TaxiSchema.from(timeTypeTest, "Test", "0.1.0")
   val schemaUpsertTest = TaxiSchema.from(upsertTest, "Test", "0.1.0")
   val schemaTemporalDownCastTest = TaxiSchema.from(temporalSchemaSource, "Test", "0.1.0")
   val schemaWithDefault = TaxiSchema.from(schemaWithDefaultValueSource, "Test", "0.1.0")
   val schemaConcat = TaxiSchema.from(schemaConcatSource, "test", "0.1.0")
   val instantSchema = TaxiSchema.from(instantFormatSource, "test", "0.1.0")
   val schemaWithConcatAndDefault = TaxiSchema.from(schemaWithDefaultValueSource, "test", "0.1.0")
   val personSchema = TaxiSchema.from(personSchemaSource, "test", "0.1.0")
}


