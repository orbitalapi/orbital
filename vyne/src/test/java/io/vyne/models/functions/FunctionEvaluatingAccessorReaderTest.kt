package io.vyne.models.functions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
//import io.vyne.testVyne
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Test
/*
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

 */
