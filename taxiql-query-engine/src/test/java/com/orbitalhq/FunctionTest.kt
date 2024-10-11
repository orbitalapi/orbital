package com.orbitalhq

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FunctionTest {
   @Test
   fun `can pass default values to function`():Unit = runBlocking {
      val (vyne) = testVyne("""
         function sayHello(name: String, upperName:String = name.upperCase()):String -> concat(name, upperName)
      """.trimIndent())
      val result = vyne.query("""find { sayHello('jimmy') }""")
         .firstTypedInstace().toRawObject()
      result.shouldBe("jimmyJIMMY")
   }
}
