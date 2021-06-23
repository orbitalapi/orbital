package io.vyne

import com.winterbe.expekt.should
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
         .typedObjects()
      discovered.should.have.size(2)
   }
}
