package io.vyne.tradeCompliance.aggregator

import com.winterbe.expekt.expect
import io.vyne.VyneClient
import io.vyne.tradeCompliance.Price
import io.vyne.tradeCompliance.TradeRequest
import org.junit.Before

class TradeComplianceEvaluatorTest {

   lateinit var evaluator: TradeComplianceEvaluator
   @Before
   fun setup() {
      evaluator = TradeComplianceEvaluator(
         VyneClient("http://localhost:9022")
      )
   }

   // This test requires the test harness to be running.
//   @Test

   fun canGatherResults() {
      val tradeRequest = TradeRequest(
         1_000_000.toBigDecimal(),
         "kevin",
         "jimmy",
         Price("GBP", 0.98.toBigDecimal())
      )

      val result = evaluator.evaluate(tradeRequest)
      expect(result.results).to.have.size(3)
   }
}
