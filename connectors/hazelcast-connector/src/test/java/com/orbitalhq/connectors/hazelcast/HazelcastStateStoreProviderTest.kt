package com.orbitalhq.connectors.hazelcast

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.caching.calculateStateStoreId
import com.orbitalhq.query.caching.getIdFields
import com.orbitalhq.query.caching.mergeNonNullValues
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveKeys
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class HazelcastStateStoreProviderTest : DescribeSpec({
   val schema = TaxiSchema.from(
      """
            model Person {
               @Id personId : PersonId inherits String
               name : Name inherits String
               age : Age inherits Int
           }
           model Cat {
               name : Name
            }
         """.trimIndent()
   )
   describe("calculating state store item key") {
      it("uses an @Id annotated field") {
         getIdFields(schema.type("Person"))
            .shouldHaveKeys("personId")
         getIdFields(schema.type("Cat"))
            .shouldBeEmpty()
      }
      it("creates a key based on @Id fields") {
         val instance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : "Jimmy" , "age" : 22 }""")
         calculateStateStoreId(instance, schema).shouldBe("personId:24601")
      }
      it("returns null when the @Id field is null") {
         val instance = parseJson(schema, "Person", """{ "personId" : null, "name" : "Jimmy" }""")
         calculateStateStoreId(instance, schema).shouldBeNull()
      }
      it("returns null if there are no @Id fields") {
         val instance = parseJson(schema, "Cat", """{ "name" : "Jimmy" }""")
         calculateStateStoreId(instance, schema).shouldBeNull()
      }
   }

   describe("Merging Typed Instances") {
      it("should merge non-null values from new onto old") {
         val oldInstance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : "Jimmy" , "age" : null }""")
         val newInstance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : null , "age" : 22 }""")
         val merged = mergeNonNullValues(oldInstance, newInstance, schema)
         merged.toRawObject()
            .shouldBe(
               mapOf(
                  "personId" to "24601",
                  "name" to "Jimmy",
                  "age" to 22
               )
            )
      }
      it("should replace non-null values from new onto old") {
         val oldInstance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : "Jimmy" , "age" : 21 }""")
         val newInstance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : null , "age" : 22 }""")
         mergeNonNullValues(oldInstance, newInstance, schema)
            .toRawObject()
            .shouldBe(
               mapOf(
                  "personId" to "24601",
                  "name" to "Jimmy",
                  "age" to 22
               )
            )
      }
   }
})
