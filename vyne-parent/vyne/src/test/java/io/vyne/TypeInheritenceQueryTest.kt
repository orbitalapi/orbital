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

type FxSwap {
   nearLegNotional : NearLegNotional inherits Notional
   farLegNotional : FarLegNotional inherits Notional
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
      val discovered = vyne.query().gather("Notional")
      expect(discovered.isFullyResolved).to.be.`true`
      expect(discovered.results).to.have.size(1)
      val collection = discovered["Notional"] as TypedCollection
      expect(collection.size).to.equal(2)
   }
}
