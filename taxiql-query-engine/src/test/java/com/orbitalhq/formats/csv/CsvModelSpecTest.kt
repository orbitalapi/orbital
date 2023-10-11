package com.orbitalhq.formats.csv

import com.winterbe.expekt.should
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.testVyne
import org.junit.Test

class CsvModelSpecTest {

   @Test
   fun `can specify csv format on model spec`() {
      val (vyne,_) = testVyne("""
         @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL"
         )
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """.trimIndent())
      val csv = """firstName|lastName|age
jimmy|jones|23
jack|johnson|NULL
      """
      val result = TypedInstance.from(
         type = vyne.type("Person[]"),
         value = csv,
         schema = vyne.schema,
         formatSpecs = listOf(CsvFormatSpec))
      result.should.be.instanceof(TypedCollection::class.java)
   }
}
