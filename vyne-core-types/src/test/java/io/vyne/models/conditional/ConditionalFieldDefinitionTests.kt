package io.vyne.models.conditional

import com.winterbe.expekt.should
import io.vyne.models.FailedEvaluation
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
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
