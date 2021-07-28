package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJson
import io.vyne.testVyne
import org.junit.Test

class ExpressionTest {

   @Test
   fun `can evaluate simple expression type`() {
      val (vyne,_) = testVyne("""
         type Height inherits Int
         type Width inherits Int
//       type Area inherits Int by Height * Width
         model Rectangle {
            height : Height
            width : Width
            area : Area
         }
      """.trimIndent())
      val instance = vyne.parseJson("Rectangle", """{ "height" : 5 , "width" : 10 }""") as TypedObject
      instance.toRawObject().should.equal(mapOf(
         "height" to 5,
         "width" to 10,
         "area" to 50
      ))
   }
}
