package com.orbitalhq.functions.stdlib.transform

import com.orbitalhq.rawObjects
import com.orbitalhq.testVyne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.runBlocking
import lang.taxi.CompilationException
import lang.taxi.shouldContainMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapTest {
   @Test
   fun `can use map to convert items in an array`(): Unit = runBlocking {
      val (vyne) = testVyne("""
         model Film {
            title : Title inherits String
            year : Year inherits Int
         }
      """.trimIndent())
      val result = vyne.query("""
         given { Film[] = [{ title: "Star Wars", year: 1979 }, { title: "Jaws", year: 1984 }] }
         find { Film[].map ( (Title) -> Title.upperCase() ) }
      """.trimIndent())
         .rawResults.toList()
      result.shouldNotBeNull()
      result.shouldBe(listOf("STAR WARS", "JAWS"))
   }

   @Test
   fun `mapping a non-array type throws compiler error`(): Unit = runBlocking {
      val (vyne) = testVyne("""
         model Film {
            title : Title inherits String
            year : Year inherits Int
         }
      """.trimIndent())

      val exception = assertThrows<CompilationException> {
         vyne.query("""
         given { Film = { title: "Star Wars", year: 1979 } }
         find { Film.map ( (Title) -> Title.upperCase() ) }
      """)
            .rawResults.toList()
      }
      exception.errors.shouldContainMessage("Type mismatch. Type of Film is not assignable to type lang.taxi.Array<taxi.stdlib.map\$T>")
   }
}
