package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

class CollectAllInstancesSpec : DescribeSpec({
   describe("collectAllInstances") {
      val schema = """ model Address {
               street : Street inherits String
            }
            model Person {
               name : Name inherits String
               addresses : Address[]
               workAddress : Address[]
             }
             service PersonApi {
               write operation getPerson():Person
            }"""
      it("Merges all collections") {
         val (vyne, stub) = testVyne(schema)
         stub.addResponse(
            "getPerson", """
            {
               "name" : "Jimmy",
               "addresses": [
                 { "street" : "Oxford St" },
                 { "street" : "Regent St" }
               ],
               "workAddress": [
                  { "street" : "Tot St" },
                  { "street" : "Bit St" },
                  { "street" : "Tut St" }
               ]
              }
            }
         """.trimIndent()
         )
         val result = vyne.query(
            """
            call PersonApi::getPerson as {
               name : Name
               addr: collectAllInstances(Address) as {
                  st: Street
               }[] ?: []
            }
         """.trimIndent()
         )
            .firstRawObject()
         result["addr"].shouldBeInstanceOf<List<*>>()
            .shouldHaveSize(5)
      }

      it("Returns an empty collection when there is only null") {
         val (vyne, stub) = testVyne(schema)
         stub.addResponse(
            "getPerson", """
            {
               "name" : "Jimmy",
               "addresses": null,
               "workAddress": null
              }
            }
         """.trimIndent()
         )
         val result = vyne.query(
            """
            call PersonApi::getPerson as {
               name : Name
               addr: collectAllInstances(Address) as {
                  st: Street
               }[] ?: []
            }
         """.trimIndent()
         )
            .firstRawObject()
         result["addr"]
            .shouldNotBeNull()
            .shouldBeInstanceOf<List<*>>()
            .shouldBeEmpty()
      }
      it("Returns an empty collection all sources are empty") {
         val (vyne, stub) = testVyne(schema)
         stub.addResponse(
            "getPerson", """
            {
               "name" : "Jimmy",
               "addresses": [],
               "workAddress": []
              }
            }
         """.trimIndent()
         )
         val result = vyne.query(
            """
            call PersonApi::getPerson as {
               name : Name
               addr: collectAllInstances(Address) as {
                  st: Street
               }[] ?: []
            }
         """.trimIndent()
         )
            .firstRawObject()
         result["addr"]
            .shouldNotBeNull()
            .shouldBeInstanceOf<List<*>>()
            .shouldBeEmpty()
      }
      it("Combines a null source with a populated source") {
         val (vyne, stub) = testVyne(schema)
         stub.addResponse(
            "getPerson", """
            {
               "name" : "Jimmy",
              "addresses": [
                 { "street" : "Oxford St" },
                 { "street" : "Regent St" }
               ],
               "workAddress": null
              }
            }
         """.trimIndent()
         )
         val result = vyne.query(
            """
            call PersonApi::getPerson as {
               name : Name
               addr: collectAllInstances(Address) as {
                  st: Street
               }[] ?: []
            }
         """.trimIndent()
         )
            .firstRawObject()
         result["addr"]
            .shouldBeInstanceOf<List<*>>()
            .shouldHaveSize(2)
      }
   }
})
