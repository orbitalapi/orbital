package com.orbitalhq.functions.stdlib.parsers

import com.orbitalhq.firstRawObject
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ParseJsonSpec : DescribeSpec({
   describe("stdlib JSON parsing") {
      it("should parse inner json strings") {
         val message = """ { "name" : "Jimmy", "payload" : "{ \"message\" : \"Hello\" }" }"""
         val schema = TaxiSchema.from("""
            model Payload {
               message : String
            }
            model Event {
               name : String
               payload : String
               parsed : Payload = parseJson(this.payload, Payload)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Event", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "name" to "Jimmy",
            "payload" to """{ "message" : "Hello" }""",
            "parsed" to mapOf("message" to "Hello")
         ))
      }

      it("should evaluate functions when parsing inner json strings") {
         val message = """ { "name" : "Jimmy", "payload" : "{ \"message\" : \"Hello\" }" }"""
         val schema = TaxiSchema.from("""
            model Payload {
               message : String
               upperMessage: String = this.message.upperCase()
            }
            model Event {
               name : String
               payload : String
               parsed : Payload = parseJson(this.payload, Payload)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Event", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "name" to "Jimmy",
            "payload" to """{ "message" : "Hello" }""",
            "parsed" to mapOf("message" to "Hello", "upperMessage" to "HELLO")
         ))
      }

      it("should parse json with numeric, boolean, and null values") {
         val message = """ { "data" : "{ \"count\": 42, \"price\": 19.99, \"active\": true, \"label\": null }" }"""
         val schema = TaxiSchema.from("""
            model Stats {
               count : Int
               price : Decimal
               active : Boolean
               label : String?
            }
            model Wrapper {
               data : String
               stats : Stats = parseJson(this.data, Stats)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "data" to """{ "count": 42, "price": 19.99, "active": true, "label": null }""",
            "stats" to mapOf("count" to 42, "price" to 19.99.toBigDecimal(), "active" to true, "label" to null)
         ))
      }

      it("should parse json with nested objects") {
         val message = """ { "payload" : "{ \"address\": { \"city\": \"London\", \"country\": \"UK\" } }" }"""
         val schema = TaxiSchema.from("""
            model Address {
               city : String
               country : String
            }
            model Person {
               address : Address
            }
            model Wrapper {
               payload : String
               person : Person = parseJson(this.payload, Person)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "address": { "city": "London", "country": "UK" } }""",
            "person" to mapOf("address" to mapOf("city" to "London", "country" to "UK"))
         ))
      }

      it("should parse json with arrays") {
         val message = """ { "payload" : "{ \"tags\": [\"a\", \"b\", \"c\"] }" }"""
         val schema = TaxiSchema.from("""
            model Tagged {
               tags : String[]
            }
            model Wrapper {
               payload : String
               tagged : Tagged = parseJson(this.payload, Tagged)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "tags": ["a", "b", "c"] }""",
            "tagged" to mapOf("tags" to listOf("a", "b", "c"))
         ))
      }

      it("should parse json with an array of objects") {
         val message = """ { "payload" : "{ \"items\": [{ \"name\": \"A\" }, { \"name\": \"B\" }] }" }"""
         val schema = TaxiSchema.from("""
            model Item {
               name : String
            }
            model Container {
               items : Item[]
            }
            model Wrapper {
               payload : String
               container : Container = parseJson(this.payload, Container)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "items": [{ "name": "A" }, { "name": "B" }] }""",
            "container" to mapOf("items" to listOf(mapOf("name" to "A"), mapOf("name" to "B")))
         ))
      }

      it("should handle empty json object") {
         val message = """ { "payload" : "{}" }"""
         val schema = TaxiSchema.from("""
            model Empty {
            }
            model Wrapper {
               payload : String
               parsed : Empty = parseJson(this.payload, Empty)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message) as TypedObject
         instance["parsed"].typeName.shouldBe("Empty")

         // Can't decide what the correct behaviour here shuold be
         // ie - what's the serialized value of a TypedInstance with no
         // properties? We treat scalar and object values differently,
         // so need to decide what an object with no properties is, and
         // how to treat it.
//         instance.shouldBe(mapOf(
//            "payload" to "{}",
//            "parsed" to emptyMap<String, Any>()
//         ))
      }
      it("should handle null json object") {
         val message = """ { "payload" : null }"""
         val schema = TaxiSchema.from("""
            model Empty {
            }
            model Wrapper {
               payload : String
               parsed : Empty = parseJson(this.payload, Empty)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to null,
            "parsed" to null
         ))
      }


      it("should ignore extra fields in json that are not in the target model") {
         val message = """ { "payload" : "{ \"name\": \"Alice\", \"age\": 30, \"extra\": \"ignored\" }" }"""
         val schema = TaxiSchema.from("""
            model Person {
               name : String
            }
            model Wrapper {
               payload : String
               person : Person = parseJson(this.payload, Person)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "name": "Alice", "age": 30, "extra": "ignored" }""",
            "person" to mapOf("name" to "Alice")
         ))
      }

      it("should handle missing optional fields in parsed json") {
         val message = """ { "payload" : "{ \"name\": \"Alice\" }" }"""
         val schema = TaxiSchema.from("""
            model Person {
               name : String
               nickname : String?
            }
            model Wrapper {
               payload : String
               person : Person = parseJson(this.payload, Person)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "name": "Alice" }""",
            "person" to mapOf("name" to "Alice", "nickname" to null)
         ))
      }

      it("should return null when source string is null") {
         val (vyne, _) = testVyne("""
            model Payload {
               message : String
            }
            model Event {
               name : String
               payload : String?
               parsed : Payload? = parseJson(this.payload, Payload)
            }
         """.trimIndent())
         val result = vyne.query("""
            given {
               event: Event = {
                  name: 'test',
                  payload: null
               }
            }
            find {
               name: event.name
               parsed: event.parsed
            }
         """.trimIndent())
            .firstRawObject()
         result["name"].shouldBe("test")
         result["parsed"].shouldBe(null)
      }

      it("should handle deeply nested json-in-json") {
         val innerJson = """{ \\\"value\\\": \\\"deep\\\" }"""
         val message = """ { "outer" : "{ \"inner\": \"{ \\\"value\\\": \\\"deep\\\" }\" }" }"""
         val schema = TaxiSchema.from("""
            model Deep {
               value : String
            }
            model Middle {
               inner : String
               deep : Deep = parseJson(this.inner, Deep)
            }
            model Outer {
               outer : String
               middle : Middle = parseJson(this.outer, Middle)
            }
         """.trimIndent())
         @Suppress("UNCHECKED_CAST")
         val instance = parseJson(schema, "Outer", message)
            .toRawObject() as Map<String, Any?>
         val middle = instance["middle"] as Map<String, Any?>
         val deep = middle["deep"] as Map<String, Any?>
         deep["value"].shouldBe("deep")
      }

      it("should parse json with semantic types") {
         val message = """ { "payload" : "{ \"firstName\": \"Alice\", \"age\": 30 }" }"""
         val schema = TaxiSchema.from("""
            type FirstName inherits String
            type Age inherits Int
            model Person {
               firstName : FirstName
               age : Age
            }
            model Wrapper {
               payload : String
               person : Person = parseJson(this.payload, Person)
            }
         """.trimIndent())
         val instance = parseJson(schema, "Wrapper", message)
            .toRawObject()
         instance.shouldBe(mapOf(
            "payload" to """{ "firstName": "Alice", "age": 30 }""",
            "person" to mapOf("firstName" to "Alice", "age" to 30)
         ))
      }
   }
})
