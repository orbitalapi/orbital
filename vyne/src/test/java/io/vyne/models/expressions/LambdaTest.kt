package io.vyne.models.expressions

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
            type FoodIsInAcceptableCalorieRange inherits Boolean by (MinimumAcceptableCalories , MaximumAcceptableCalories) -> ProductCalories > MinimumAcceptableCalories && ProductCalories < MaximumAcceptableCalories

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
}
