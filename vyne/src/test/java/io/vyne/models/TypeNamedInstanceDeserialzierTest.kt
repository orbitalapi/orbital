package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import org.junit.Test

class TypeNamedInstanceDeserialzierTest {
   @Test
   fun canDeserializeArrayOfTypeNamedInstances() {
      val result = jacksonObjectMapper()
         .readValue<List<TypeNamedInstance>>(arrayBased)
      expect(result).to.have.size(2)
   }

   @Test
   fun canDeserializeObjectWithTypeNamedInstances() {
      val result = jacksonObjectMapper()
         .readValue<TypeNamedInstance>(asObject)
      // TODO : Actually do some assertions here.
      expect(result).to.be.not.`null`
   }


}

private val asObject = """{
   "typeName": "io.vyne.demos.marketing.shop.AvailableRewards",
   "value": {
      "rewards": [
         {
            "typeName": "io.vyne.demos.marketing.shop.Reward",
            "value": {
               "name": {
                  "typeName": "lang.taxi.String",
                  "value": "Weekend at the spa"
               },
               "priceInGbp": {
                  "typeName": "lang.taxi.Int",
                  "value": 300
               }
            }
         },
         {
            "typeName": "io.vyne.demos.marketing.shop.Reward",
            "value": {
               "name": {
                  "typeName": "lang.taxi.String",
                  "value": "Night at the moview"
               },
               "priceInGbp": {
                  "typeName": "lang.taxi.Int",
                  "value": 20
               }
            }
         },
         {
            "typeName": "io.vyne.demos.marketing.shop.Reward",
            "value": {
               "name": {
                  "typeName": "lang.taxi.String",
                  "value": "Bottle of wine"
               },
               "priceInGbp": {
                  "typeName": "lang.taxi.Int",
                  "value": 10
               }
            }
         }
      ]
   }
}
""".trimIndent()
private val arrayBased = """
[
   {
      "typeName": "demos.DiscountPromotion",
      "value": {
         "message": {
            "typeName": "lang.taxi.String",
            "value": "Hey, Jimmy, get 20% off today!  Hurry, offer ends soon"
         },
         "title": {
            "typeName": "lang.taxi.String",
            "value": "AwesomeDiscount"
         },
         "availableUntil": {
            "typeName": "lang.taxi.Date",
            "value": "2019-07-08"
         }
      }
   },
   {
      "typeName": "demo.DoublePointsPromotion",
      "value": {
         "message": {
            "typeName": "lang.taxi.String",
            "value": "Hey, Jimmy, you could double your points - that's an extra 2300 points, just by shopping at one of these stores:"
         },
         "shops": [
            {
               "typeName": "lang.taxi.String",
               "value": "Twin Pines"
            },
            {
               "typeName": "lang.taxi.String",
               "value": "Lone Pines"
            },
            {
               "typeName": "lang.taxi.String",
               "value": "Twin Peaks"
            }
         ],
         "title": {
            "typeName": "lang.taxi.String",
            "value": "EarnDoublePoints"
         },
         "availableUntil": {
            "typeName": "lang.taxi.Date",
            "value": "2019-07-12"
         }
      }
   }
]
""".trimIndent().trim()
