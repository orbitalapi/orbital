package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.json.addJsonModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
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
   fun queryingForAnAliasedTypeShouldReturnAllOtherAliases() = runBlockingTest{
      val (vyne, _) = testVyne(schema)
      val json = """
{
   nearLegNotional : 1000,
   farLegNotional : 1500
}
      """.trimIndent()
      vyne.addJsonModel("FxSwap", json)
      val discovered =  vyne.query().findAll("Notional")
      expect(discovered.isFullyResolved).to.be.`true`
      val collection = discovered.typedInstances().first() as TypedCollection
      collection.should.have.size(2)
   }
}
