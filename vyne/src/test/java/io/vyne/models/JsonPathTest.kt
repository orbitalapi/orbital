package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class JsonPathTest {
   val traderJson = """
       {
         "username" : "EUR_Trader",
         "jurisdiction" : "EUR",
         "limit" : {
            "currency" : "USD",
            "value" : 100
         }
       }
   """.trimIndent()
   @Test
   fun canReadValueFromJsonPath() {
      val taxi = TaxiSchema.from("""
         type Foo {
            limitValue : Decimal by jsonPath("$.limit.value")
         }
      """.trimIndent())
      val instance = TypedInstance.from(taxi.type("Foo"), traderJson, schema = taxi, source = Provided) as TypedObject
      instance["limitValue"].value.should.equal(100.toBigDecimal())
   }

   @Test
   fun canReadJsonPathWithComplexArrayAccess() {
      // Usecase here is finding a specific array element.
      // Jsonpath doesn't support parent access, but Jayway's JsonPath issues page
      // suggests this approach.
      // See : https://github.com/json-path/JsonPath/issues/287#issuecomment-265479196
      // This stems from a client trying to parse a Json FIX message
      val json = """{
  "noSecurityAltID": [
    6,
    {
      "securityAltID": "GEZ0",
      "securityAltIDSource": [
        "98",
        "NAME"
      ]
    },
    {
      "securityAltID": "GE Dec20",
      "securityAltIDSource": [
        "97",
        "ALIAS"
      ]
    },
    {
      "securityAltID": "1EDZ0",
      "securityAltIDSource": [
        "5",
        "RIC_CODE"
      ]
    },
    {
      "securityAltID": "870833",
      "securityAltIDSource": [
        "8",
        "EXCHANGE_SECURITY_ID"
      ]
    },
    {
      "securityAltID": "EDZ0 Comdty",
      "securityAltIDSource": [
        "A",
        "BLOOMBERG_CODE"
      ],
      "bloombergSecurityExchange": "CME"
    },
    {
      "securityAltID": "BBG001BH7R55",
      "securityAltIDSource": [
        "S",
        "OPENFIGI_ID"
      ],
      "bloombergSecurityExchange": "CME"
    }
  ]
}"""
      val taxi = TaxiSchema.from("""
         type RicCode inherits String
         model FixMessage {
            ricCode : RicCode by jsonPath("$.noSecurityAltID[?(@.securityAltIDSource[1]=='RIC_CODE')].securityAltID")
         }
      """.trimIndent())
      val instance = TypedInstance.from(taxi.type("FixMessage"), json, taxi, source = Provided) as TypedObject
      instance["ricCode"].value.should.equal("1EDZ0")
   }

   @Test
   fun jsonPathToUndefinedValueReturnsNull() {
      val taxi = TaxiSchema.from("""
         type Foo {
            limitValue : Decimal by jsonPath("$.something.that.doesnt.exist")
         }
      """.trimIndent())
      val instance = TypedInstance.from(taxi.type("Foo"), traderJson, schema = taxi, source = Provided) as TypedObject
      instance["limitValue"].value.should.be.`null`
   }

}
