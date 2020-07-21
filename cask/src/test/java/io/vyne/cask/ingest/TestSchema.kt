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

   val schemaTimeTest = TaxiSchema.from(timeTypeTest, "Test", "0.1.0")
   val schemaUpsertTest = TaxiSchema.from(upsertTest, "Test", "0.1.0")
}
