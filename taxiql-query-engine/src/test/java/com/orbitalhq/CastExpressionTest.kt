package com.orbitalhq

import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import io.kotest.matchers.shouldBe
import org.junit.Test

class CastExpressionTest {

   @Test
   fun `can cast between compatible types`() {
      val (vyne,stub) = testVyne("""
         type ReleaseYear inherits Int
         type PublicDomainYear inherits Int
         model Movie {
            released : ReleaseYear
            isCopyright : Boolean
            publicDomain : PublicDomainYear = when {
               this.isCopyright -> this.released + 30
               else -> (PublicDomainYear) this.released
            }
         }
      """)
      val instance = vyne.parseJson("Movie", """{ "released" : 2020, "isCopyright" : true}""") as TypedObject
      instance["publicDomain"].value.shouldBe(2050)
      instance["publicDomain"].type.qualifiedName.shortDisplayName.shouldBe("PublicDomainYear")

      val instance2 = vyne.parseJson("Movie", """{ "released" : 2020, "isCopyright" : false}""") as TypedObject
      instance2["publicDomain"].value.shouldBe(2020)
      instance2["publicDomain"].type.qualifiedName.shortDisplayName.shouldBe("PublicDomainYear")
   }
}
