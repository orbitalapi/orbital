package com.orbitalhq.models.conditional

import com.winterbe.expekt.should
import com.orbitalhq.models.FailedEvaluation
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.Test
import kotlin.test.assertFailsWith

class ConditionalFieldDefinitionTests {
   @Test
   fun `when conditional expression does not have a required else statement`() {
       val testString = "";
      val schema = TaxiSchema.from("""
         model Foo {
            age : Int by jsgonPath("/age")
            ageStr: String by when (this.age)  {
               1 -> "1 year old"
            }
         }
      """.trimIndent()

      )
      val typedObject = TypedInstance.from(schema.type("Foo"), """{ "age": 2 } """, schema, source = Provided) as TypedObject
      val nullValue = typedObject["ageStr"] as TypedNull
      val failedEvaluation =  nullValue.source as FailedEvaluation
      failedEvaluation.message.should.contain("No matching cases found in when clause")
   }
}
