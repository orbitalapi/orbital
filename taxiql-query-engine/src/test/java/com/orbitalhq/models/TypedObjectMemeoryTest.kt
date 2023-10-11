package com.orbitalhq.models

import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.timed

class TypedObjectMemoryTest {

   // Excluding this test from the build, as it's slow, and just tests memory allocation.
//   @Test
   fun buildLotsOfObjects() {
      val fieldCount = 100
      val fields = (1..fieldCount).joinToString(separator = "\n") { "field$it : Field$it inherits String" }
      val schema = TaxiSchema.from(
         """
         model BigModel {
            $fields
         }
      """.trimIndent()
      )

      System.gc()
      val runtime = Runtime.getRuntime()
      val usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory()


      val instanceCount = 500_000
      val instances = timed("Building instances") {
         (1..instanceCount).map {
            val value = (1..fieldCount).map { "field$it" to "Value-$it" }
               .toMap()
            value
//         TypedInstance.from(schema.type("BigModel"), value, schema)
         }
      }


      System.gc()
      val usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory()
      val increaseInMb = (usedMemoryAfter - usedMemoryBefore) * 0.000001

      println("Allocating ${instances.size} increased memory: $increaseInMb MB")
   }
}
