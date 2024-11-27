package com.orbitalhq.query.state

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.caching.InvalidStateJoinTypesException
import com.orbitalhq.query.caching.StateStoreIdProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class StateStoreIdProviderTest : DescribeSpec({

   describe("calculating state store item key") {
      it("id field on one type present as non-id field on other type") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
               customerId : CustomerId inherits String
           }
           model CustomerOrders {
               customerId : CustomerId
               orderId : OrderId
            }
          """.trimIndent()
         )
         val provider = StateStoreIdProvider(setOf(schema.type("Order"), schema.type("CustomerOrders")), schema)
         provider.getIdFieldNames(schema.type("Order"))
            .values
            .shouldBe(listOf("id"))
         provider.getIdFieldNames(schema.type("CustomerOrders"))
            .values
            .shouldBe(listOf("orderId"))
      }
      it("both types have different id fields but only one type has both types") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
           }
           // This has it's own Id - CustomerId
           // However the CustomerId isn't present on Order, so can't be used
           // for joining
           model CustomerOrder {
               @Id
               customerId : CustomerId
               orderId : OrderId
            }
          """.trimIndent()
         )
         val provider = StateStoreIdProvider(setOf(schema.type("Order"), schema.type("CustomerOrder")), schema)
         provider.getIdFieldNames(schema.type("Order"))
            .values
            .shouldBe(listOf("id"))
         provider.getIdFieldNames(schema.type("CustomerOrder"))
            .values
            .shouldBe(listOf("orderId"))
      }
      it("selects a common @Id tagged field when both types have same id fields tagged with @Id") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
           }
           model CustomerOrder {
               customerId : CustomerId
               @Id
               orderId : OrderId
            }
          """.trimIndent()
         )
         val provider = StateStoreIdProvider(setOf(schema.type("Order"), schema.type("CustomerOrder")), schema)
         provider.getIdFieldNames(schema.type("Order"))
            .values
            .shouldBe(listOf("id"))
         provider.getIdFieldNames(schema.type("CustomerOrder"))
            .values
            .shouldBe(listOf("orderId"))
      }

      it("throws meaningful error if ids are ambiguous") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
               customerId : CustomerId inherits String
           }
           model CustomerOrder {
               @Id
               customerId : CustomerId
               orderId : OrderId
            }
          """.trimIndent()
         )
         val exception = shouldThrow<InvalidStateJoinTypesException> {
            StateStoreIdProvider(setOf(schema.type("Order"), schema.type("CustomerOrder")), schema)
         }
         exception.message.shouldBe("Ambiguous join found between types Order, CustomerOrder - The following fields could be used to to join on: OrderId, CustomerId.")
      }

      it("throws meaningful error if no common ids are present") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
           }
           model CustomerOrder {
               @Id
               customerId : CustomerId
            }
          """.trimIndent()
         )
         val exception = shouldThrow<InvalidStateJoinTypesException> {
            StateStoreIdProvider(setOf(schema.type("Order"), schema.type("CustomerOrder")), schema)
         }
         exception.message.shouldBe("No valid joins available between the types of Order, CustomerOrder - No types contain an @Id annotated field that is also present on the other types")
      }

      // This isn't implemented yet
      xit("uses an annotation on a union type to resolve ambiguous joins") {
         val schema = TaxiSchema.from(
            """
            model Order {
               @Id id : OrderId inherits String
               customerId : CustomerId inherits String
           }
           model CustomerOrder {
               @Id
               customerId : CustomerId
               orderId : OrderId
            }

            @Join(joinTypes = ['OrderId'])
            type CustomerOrderJoin by Order | CustomerOrder
          """.trimIndent()
         )
         shouldThrow<InvalidStateJoinTypesException> {
            StateStoreIdProvider(setOf(schema.type("CustomerOrderJoin")), schema)
         }
      }

//       it("uses an @Id annotated field") {
//          getIdFields(schema.type("Person"))
//             .shouldHaveKeys("personId")
//          getIdFields(schema.type("Cat"))
//             .shouldBeEmpty()
//       }
      describe("id key generation") {
         val schema = TaxiSchema.from(
            """
            model Person {
               @Id personId : PersonId inherits String
               name : Name inherits String
               age : Age inherits Int
           }
           model Tweet {
               name : Name
               author : PersonId
            }
         """.trimIndent()
         )
         it("creates a key based on @Id fields") {
            val instance = parseJson(schema, "Person", """{ "personId" : "24601", "name" : "Jimmy" , "age" : 22 }""")
            val idProvider = StateStoreIdProvider(setOf(schema.type("Person"), schema.type("Tweet")), schema)
            idProvider.calculateStateStoreId(instance).shouldBe("PersonId:24601")
         }
         it("returns null when the @Id field is null") {
            val instance = parseJson(schema, "Person", """{ "personId" : null, "name" : "Jimmy" }""")
            val idProvider = StateStoreIdProvider(setOf(schema.type("Person"), schema.type("Tweet")), schema)
            idProvider.calculateStateStoreId(instance).shouldBeNull()
         }
      }

   }
})
