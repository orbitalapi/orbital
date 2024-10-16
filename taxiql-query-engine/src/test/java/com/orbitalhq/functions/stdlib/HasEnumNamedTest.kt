package com.orbitalhq.functions.stdlib

import com.orbitalhq.firstRawValue
import com.orbitalhq.testVyne
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HasEnumNamedTest {

   @Test
   fun`enum contains`():Unit = runBlocking {
      val (vyne) = testVyne("""
         enum Countries {
            default NZ,
            AUS
         }
      """.trimIndent())
      vyne.query("""find { Countries.hasEnumNamed('NZ') }""")
         .firstRawValue()!!.shouldBe(true)
      vyne.query("""find { Countries.hasEnumNamed('AUS') }""")
         .firstRawValue()!!.shouldBe(true)
      vyne.query("""find { Countries.hasEnumNamed('UK') }""")
         .firstRawValue()!!.shouldBe(false)
   }
}
