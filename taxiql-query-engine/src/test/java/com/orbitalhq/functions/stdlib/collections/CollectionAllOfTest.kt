package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CollectionAllOfTest : DescribeSpec({
   describe("all any none std lib functions") {
      it("should evaluate against a collection") {
         val (vyne,_ ) = testVyne("""
            model Transaction {
               declined : Declined inherits Boolean
               disputed : Disputed inherits Boolean
               completed : Completed inherits Boolean
            }

         """.trimIndent())
         val result = vyne.query("""
            given {
               transactions : Transaction[] = [
                  { declined : false, disputed: false, completed: true },
                  { declined : false, disputed: true, completed: true }
               ]
            }
            find {
               anyDeclined: Boolean = transactions.any( (Declined) -> Declined )
               noneDeclined: Boolean = transactions.none( (Declined) -> Declined )
               allCompleted: Boolean = transactions.all( (Completed) -> Completed )
               noneCompleted : Boolean = transactions.none( (Completed) -> Completed )
               allDeclined: Boolean = transactions.all( (Declined) -> Declined )
               anyDisputed: Boolean = transactions.any( (Disputed) -> Disputed )
            }
         """.trimIndent())
            .firstRawObject()
         result.shouldBe(mapOf(
            "anyDeclined" to false,
            "noneDeclined" to true,
            "allCompleted" to true,
            "noneCompleted" to false,
            "allDeclined" to false,
            "anyDisputed" to true,
         ))
      }
   }
})
