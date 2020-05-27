package io.vyne

import com.winterbe.expekt.expect
import io.vyne.models.TypedCollection
import io.vyne.models.json.addJsonModel
import org.junit.Test

class TypeInheritenceQueryTest {
   val schema = """
type FxSingleLeg {
   notional : Notional as Decimal
}

type NearLegNotional inherits Notional
type FarLegNotional inherits Notional

type FxSwap {
   nearLegNotional : NearLegNotional
   farLegNotional : FarLegNotional
}
   """.trimIndent()

   @Test
   fun queryingForAnAliasedTypeShouldReturnAllOtherAliases() {
      val (vyne, _) = testVyne(schema)
      val json = """
{
   nearLegNotional : 1000,
   farLegNotional : 1500
}
      """.trimIndent()
      vyne.addJsonModel("FxSwap", json)
      val discovered = vyne.query().findAll("Notional")
      expect(discovered.isFullyResolved).to.be.`true`
      expect(discovered.results).to.have.size(1)
      val collection = discovered["Notional"] as TypedCollection
      expect(collection.size).to.equal(2)
   }
}
