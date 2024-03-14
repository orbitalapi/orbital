package com.orbitalhq

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test


class QueryExpressionTest {
   @Test
   fun `a query can be an expression`():Unit = runBlocking {
      val (vyne,_) = testVyne()
      vyne.query("""find { 'Hello world' }""")
         .firstRawValue().shouldBe("Hello world")
   }

   @Test
   fun `a query can be an expression using variables`():Unit = runBlocking {
      val (vyne,_) = testVyne()
      vyne.query("""
         given { input:Int = 2 }
         find { input + 1 }""".trimIndent())
         .firstRawValue().shouldBe(3)
   }

}
