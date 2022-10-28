package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.firstTypedInstace
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.fail

class LambdaTest {
    @Test
    fun `can evaluate simple lambda with inputs`(): Unit = runBlocking {
        val (vyne, stub) = testVyne(
            """
            type MinimumAcceptableCalories inherits Int
            type MaximumAcceptableCalories inherits Int
            type ProductCalories inherits Int
            type FoodIsInAcceptableCalorieRange by (MinimumAcceptableCalories , MaximumAcceptableCalories) -> ProductCalories > MinimumAcceptableCalories && ProductCalories < MaximumAcceptableCalories

            model Input {
               min:MinimumAcceptableCalories
               max:MaximumAcceptableCalories
               productId : ProductId inherits Int
            }
            service FoodService {
               operation getCalories(ProductId):ProductCalories
            }
  """
        )
        stub.addResponse("getCalories", TypedInstance.from(vyne.type("ProductCalories"), 100, vyne.schema))
        data class TestParams(val testCase: String, val min: Int, val max: Int, val expected: Boolean)

        listOf(
            TestParams("Value within range should return true", 50, 120, expected = true),
            TestParams("Value too high range should return false", 200, 300, expected = false),
            TestParams("Value too low range should return false", 10, 20, expected = false),

            ).map { params ->
            val result = vyne.from(
                vyne.parseJson(
                    "Input", """{
         |"min" : ${params.min},
         |"max" : ${params.max},
         |"productId" : 1
         |}""".trimMargin()
                )
            )
                .build("FoodIsInAcceptableCalorieRange")
                .firstTypedInstace()
            if (result.value != params.expected) {
                fail("Scenario ${params.testCase} failed")
            }
        }
    }

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
