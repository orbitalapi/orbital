package io.vyne.models

import com.winterbe.expekt.expect
import lang.taxi.annotations.DataType
import org.junit.Test

class ValueReaderTest {

   @Test
   fun canReadValueFromMap() {
      val src = mapOf("foo" to "bar")
      expect(ValueReader().read(src,"foo")).to.equal("bar")
   }

   @Test
   fun canReadNullValueFromMap() {
      val src = mapOf("foo" to "bar")
      expect(ValueReader().read(src,"unknownAttribute")).to.`null`
   }

   @Test
   fun canReadValueFromObject() {
      data class SampleInput(val name:String)
      val src = SampleInput("Jimmy")
      expect(ValueReader().read(src,"name")).to.equal("Jimmy")
   }

   @Test
   fun canReadFromDataAttribute() {
      data class SampleInput(@field:DataType("com.foo.Name") val name:String)
      val src = SampleInput("Jimmy")
      expect(ValueReader().read(src,"com.foo.Name")).to.equal("Jimmy")
      expect(ValueReader().read(src,"name")).to.equal("Jimmy")
   }

}
