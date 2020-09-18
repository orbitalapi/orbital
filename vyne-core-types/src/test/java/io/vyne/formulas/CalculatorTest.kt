package io.vyne.formulas

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import lang.taxi.types.Formula
import lang.taxi.types.FormulaOperator
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.fail

class CalculatorTest {
   data class TestScenario(val inputA:Any, val inputB:Any, val operator: FormulaOperator, val expected:Any) {
      val values = listOf(inputA,inputB)
   }

   @Test
   fun numericCalculationsBehaveAsExpected() {
      val calculator = NumberCalculator()
      listOf(
         TestScenario(1, 2, FormulaOperator.Add, 3),
         TestScenario(2, -1, FormulaOperator.Add, 1),
         TestScenario(0, -1, FormulaOperator.Add, -1),
         TestScenario(3, 1, FormulaOperator.Subtract, 2),
         TestScenario(3, -1, FormulaOperator.Subtract, 4),
         TestScenario(-2, -1, FormulaOperator.Subtract, -1),
         TestScenario(1L, 2L, FormulaOperator.Add, 3L),
         TestScenario(1.5, 4.0, FormulaOperator.Add, 5.5),
         TestScenario(1.5, 4.0, FormulaOperator.Multiply, 6.0),

         // When mixing types, you'll get a BigDecimal back
         TestScenario(1.5, 4, FormulaOperator.Multiply, 6.0.toBigDecimal()),
         TestScenario(2L, 4.0, FormulaOperator.Multiply, 8.0.toBigDecimal()),
         TestScenario(4, 0.55003, FormulaOperator.Divide, 7.272330600149083.toBigDecimal()),


         TestScenario(BigDecimal.TEN, BigDecimal.valueOf(5), FormulaOperator.Multiply, BigDecimal.valueOf(50)),
         TestScenario(BigDecimal.TEN, BigDecimal.valueOf(5), FormulaOperator.Subtract, BigDecimal.valueOf(5)),
         TestScenario(BigDecimal.TEN, BigDecimal.valueOf(5), FormulaOperator.Subtract, BigDecimal.valueOf(5))
      ).map { scenario ->
         verifyCalculation(scenario, calculator)
      }
   }

   @Test
   fun stringCalculationsBehaveAsExpected() {
      val calculator = StringCalculator()
      listOf(
         TestScenario("Hello", "World", FormulaOperator.Add, "HelloWorld"),
         TestScenario("Hello", "", FormulaOperator.Add, "Hello"),
         TestScenario("Hello", " ", FormulaOperator.Add, "Hello ")
      ).map { scenario ->
         verifyCalculation(scenario, calculator)
      }
   }


   private fun verifyCalculation(scenario: TestScenario, calculator: Calculator) {
      val formula = mock<Formula> {
         on { operator }.doReturn(scenario.operator)
      }
      try {
         calculator.calculate(formula.operator, scenario.values).should.equal(scenario.expected)
      } catch (e: Throwable) {
         fail(scenario.toString() + " - " + e.message)
      }

   }
}
