package com.orbitalhq.models.functions

import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.testVyne
import org.junit.Test

class FunctionEvaluatingAccessorReaderTest {

   @Test
   fun `default vyne should include stdlib`() {
      val (vyne,_) = testVyne("")
      val function = vyne.schema.taxi.function("taxi.stdlib.left")
   }
   @Test
   fun `functions should be invoked`() {
      val (vyne,_) = testVyne("""
         type Person {
            fullName : String by jsonPath("$.name")
            title : String by left(this.fullName,3)
         }
      """)
      val sourceJson = """{ "name" : "Mr. Jimmy" }"""
      val instance = TypedInstance.from(vyne.type("Person"), sourceJson, vyne.schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("Mr.")
   }
}


