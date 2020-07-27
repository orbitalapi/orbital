package io.vyne.query

import com.winterbe.expekt.expect
import io.vyne.TestSchema
import io.vyne.models.json.addJsonModel
import io.vyne.query.formulas.CalculatorRegistry
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import java.math.BigDecimal

class CalculatedFieldScanStrategyTest {
   val taxiDef = """
namespace vyne.example

type QtyTick inherits Decimal
type QtyHit inherits Decimal

type Order {
   qtyTick: QtyTick
   qtyHit: QtyHit
}

type Invoice {
   qtyTot: Decimal as (QtyTick * QtyHit)
}

"""
   val testSchema = TaxiSchema.from(taxiDef)
   val vyne = TestSchema.vyne(QueryEngineFactory.default(), TaxiSchema.from(taxiDef))

   @Test
   fun `Given operands available in context calculated field value is set`() {
      val json = """
{
   "qtyTick" : "2",
   "qtyHit" : "200"
}"""
      vyne.addJsonModel("vyne.example.Order", json)
      val qtyTot = vyne.type("vyne.example.Invoice").attributes["qtyTot"]!!.type
      val result = CalculatedFieldScanStrategy(CalculatorRegistry()).invoke(TestSchema.typeNode(qtyTot.fullyQualifiedName, QueryParser(testSchema)), vyne.query())
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal(qtyTot.fullyQualifiedName)
      expect(result.matchedNodes.entries.first().value!!.value).to.equal(BigDecimal("400"))
   }
}
