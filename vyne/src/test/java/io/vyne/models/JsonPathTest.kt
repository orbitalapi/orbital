package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import kotlin.test.assertFailsWith

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
   fun `can parse json string from to a schema that mixes jsonpath and plain mappings`() {
      val taxi = TaxiSchema.from("""
         model Trade {
            username : String
            country : String by jsonPath("$.jurisdiction")
            limit : Int by jsonPath("$.limit.value")
         }
      """.trimIndent())
      val instance = TypedInstance.from(taxi.type("Trade"), traderJson, schema = taxi, source = Provided) as TypedObject
      instance["username"].value.should.equal("EUR_Trader")
      instance["country"].value.should.equal("EUR")
      instance["limit"].value.should.equal(100)
   }

   @Test
   fun `can parse json node from to a schema that mixes jsonpath and plain mappings`() {
      // Note that Casks pre-parse json from string to jackson jsonNode, so this needs to pass
      val taxi = TaxiSchema.from("""
         model Trade {
            username : String
            country : String by jsonPath("$.jurisdiction")
            limit : Int by jsonPath("$.limit.value")
         }
      """.trimIndent())
      val jsonNode = jacksonObjectMapper().readTree(traderJson)
      val instance = TypedInstance.from(taxi.type("Trade"), jsonNode, schema = taxi, source = Provided) as TypedObject
      instance["username"].value.should.equal("EUR_Trader")
      instance["country"].value.should.equal("EUR")
      instance["limit"].value.should.equal(100)
   }

   @Test
   fun `can parse json string from a schema that uses legacy xpath style jsonPath mappings`() {
      val taxi = TaxiSchema.from("""
         model Trade {
            username : String
            country : String by jsonPath("/jurisdiction")
            limit : Int by jsonPath("$.limit.value")
         }
      """.trimIndent())
      val instance = TypedInstance.from(taxi.type("Trade"), traderJson, schema = taxi, source = Provided) as TypedObject
      instance["username"].value.should.equal("EUR_Trader")
      instance["country"].value.should.equal("EUR")
      instance["limit"].value.should.equal(100)
   }

   @Test
   fun `when using an indefinite json path but expecting a single response then it is returned if only one item matches`() {
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

   @Test
   fun `when the jsonPath contains a syntax error then a helpful error is thrown`() {
      val taxi = TaxiSchema.from("""
         type Oscar inherits String
         model Actor {
            name : ActorName inherits String
            awards : {
               oscars: Oscar by jsonPath("oscars[0") // Intentoionally invlalid jsonPath
            }
         }
      """.trimIndent())
      val json = """[
         {
            "name" : "Tom Cruise",
             "awards" : {
               "oscars" : [ "Best Movie" ]
             }
         },
         {
            "name" : "Tom Jones",
             "awards" : {
               "oscars" : [ "Best Song" ]
             }
         }
         ]
         """
      assertFailsWith<RuntimeException>("Could not evaluate path: oscars[0 -- the path is invalid: Could not parse token starting at position 8. Expected ?, ', 0-9, *") {
         TypedInstance.from(taxi.type("Actor[]"), json, schema = taxi) as TypedCollection
      }
   }


   @Test
   fun `can use relative json path`() {
      val taxi = TaxiSchema.from("""
         type Oscar inherits String
         model Actor {
            name : ActorName inherits String
            awards : {
               oscars: Oscar by jsonPath("oscars[0]")
            }
         }
      """.trimIndent())
      val json = """[
         {
            "name" : "Tom Cruise",
             "awards" : {
               "oscars" : [ "Best Movie" ]
             }
         },
         {
            "name" : "Tom Jones",
             "awards" : {
               "oscars" : [ "Best Song" ]
             }
         }
         ]
         """
      val collection = TypedInstance.from(taxi.type("Actor[]"), json, schema = taxi) as TypedCollection
      collection.toRawObject().should.equal(listOf(
         mapOf("name" to "Tom Cruise", "awards" to mapOf("oscars" to "Best Movie")),
         mapOf("name" to "Tom Jones", "awards" to mapOf("oscars" to "Best Song")),
      ))
   }

}
