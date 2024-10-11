package com.orbitalhq.functions.stdlib.transform

import com.orbitalhq.firstTypedInstace
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.testVyne
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FoldTest {


   @Test
   fun `can evaluate a lambda type`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
        import taxi.stdlib.fold

        model Entry {
          weight:Weight inherits Int
          score:Score inherits Int
        }

        model Input {
            entries: Entry[]
        }

       type WeightedAverage by (Entry[]) -> fold(Entry[], 0, (Entry, Int) -> Int + (Weight*Score))
        """.trimIndent()
      )

      data class Entry(
         val weight:Int,
         val score:Int
      )

      val entries = listOf(Entry(10,5), Entry(2,100))
      entries.fold(0) { acc:Int, entry -> acc + (entry.weight * entry.score) }
      val inputs = vyne.parseJson("Input", """{ "entries" : [ { "weight" : 10 , "score" : 5},  {"weight" : 2 , "score" : 100}] }""")
      val result = vyne.from(inputs).build("WeightedAverage")
         .firstTypedInstace()
      result.should.not.be.`null`
   }
}
