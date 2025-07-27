package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.accessors.ProjectionFunctionScope
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CascadingFactBagTest {

   /**
    * This is intentional.
    * If a fact bag has a value of null, we shouldn't be continuing to search for other values - it's null.
    * This is important from a scoping perspective - otherwise if we continue searching up scopes to find
    * a matching value, we break scope declarations in projections.
    *
    */
   @Test
   fun `returns null for value if primary fact bag has null`() {
      val schema = TaxiSchema.from(
         """
         model Person {
            name : PersonName inherits String
            spouseName : SpouseName inherits String
         }
      """.trimIndent()
      )
      val person = TypedInstance.from(schema.type("Person"), """{ "name" : "Jimmy" }""", schema) as TypedObject
//      person["spouseName"].shouldBeInstanceOf<TypedNull>()

      val primaryFactBag = CopyOnWriteFactBag(listOf(person), schema)

      val otherSpouseName = TypedInstance.from(schema.type("SpouseName"), "Jill", schema)
      val secondaryFactBag = CopyOnWriteFactBag(listOf(otherSpouseName), schema)

      val factBag = CascadingFactBag(primaryFactBag, secondaryFactBag)
      val searchResult = factBag.getFactOrNull(schema.type("SpouseName"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      searchResult.shouldBeInstanceOf<TypedNull>()
   }

   @Test
   fun `returns null for value if scope declares value as null`() {
      val schema = TaxiSchema.from(
         """
         model Person {
            name : PersonName inherits String
            spouseName : SpouseName inherits String
         }
      """.trimIndent()
      )
      val person = TypedInstance.from(schema.type("Person"), """{ "name" : "Jimmy" }""", schema) as TypedObject
//      person["spouseName"].shouldBeInstanceOf<TypedNull>()

      val primaryFactBag = CopyOnWriteFactBag(emptyList(), schema)
         .withAdditionalScopedFacts(
            listOf(
               scopedFact(person, "person")
            ), schema
         )

      val otherSpouseName = TypedInstance.from(schema.type("SpouseName"), "Jill", schema)
      val secondaryFactBag = CopyOnWriteFactBag(listOf(otherSpouseName), schema)

      val factBag = CascadingFactBag(primaryFactBag, secondaryFactBag)
      val searchResult = factBag.getFactOrNull(schema.type("SpouseName"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      searchResult.shouldBeInstanceOf<TypedNull>()
   }

   /**
    * This test is disabled because I can't decide what the correct behaviour should be.
    * The intent is that if we find a value in a scope, we should return that value, and not go looking elsehwere.
    *
    * But, that contradicts with the concept of ANY_DEPTH_EXPECT_ONE -- which explicitly says "look at any depth".
    * I don't know what the right behaviour should be.
    *
    * This was raised whilst fixing other issues around making it possible to correctly return TypedNull from factbag searches.
    */
   @Test
   @Disabled
   fun `returns null for value if earlier scope declares value as null`() {
      val schema = TaxiSchema.from(
         """
         model Person {
            name : PersonName inherits String
            spouseName : SpouseName inherits String
         }
      """.trimIndent()
      )
      val person = TypedInstance.from(schema.type("Person"), """{ "name" : "Jimmy" }""", schema) as TypedObject
//      person["spouseName"].shouldBeInstanceOf<TypedNull>()

      val otherSpouseName = TypedInstance.from(schema.type("SpouseName"), "Jill", schema)
      val primaryFactBag = CopyOnWriteFactBag(emptyList(), schema)
         .withAdditionalScopedFacts(
            listOf(
               scopedFact(person, "person"),
               scopedFact(otherSpouseName, "spouse")
            ), schema
         )


      val factBag = CascadingFactBag(primaryFactBag, CopyOnWriteFactBag(emptyList(), schema))
      val searchResult = factBag.getFactOrNull(schema.type("SpouseName"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      searchResult.shouldBeInstanceOf<TypedNull>()
   }


   // ORB-995
   @Test
   fun `cannot have same scoped fact in fact bag multiple times`() {
      // ORB-995 was raised because the query engine combines fact bags, and it was duplicating
      // scoped facts.
      // Hard to test that specific scenario here, but this test asserts that
      // scoped facts are not duplicated.
      val schema = TaxiSchema.from(
         """
         model Person {
            name : PersonName inherits String
         }
      """.trimIndent()
      )
      val personType = schema.type("Person")
      val person = TypedInstance.from(personType, """{ "name" : "Jimmy" }""", schema) as TypedObject
      val primaryFactBag = CopyOnWriteFactBag(emptyList(), schema, listOf(scopedFact(person, "person")))
         .withAdditionalScopedFacts(
            // Cannot re-add person scope
            listOf(
               scopedFact(person, "person"),
            ), schema
         )

      val presentFacts = primaryFactBag.getFactOrTypedNull(FactSearch.findType(personType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY))
      presentFacts.getOrNull().shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(1)


      // Try to create a cascading fact bag with the scoped fact present twice.
      val factBag = CascadingFactBag(primaryFactBag, CopyOnWriteFactBag(emptyList(), schema, listOf(scopedFact(person, "Person"))))
      val searchResult = factBag.getFactOrNull(personType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
      searchResult.shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(1)

    // Look for an attribute of the duplicated fact - PersonName
      val attributeSearchResult = factBag.getFactOrNull(schema.type("PersonName"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
      attributeSearchResult.shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(1)

   }

}


fun scopedFact(fact: TypedInstance, scopeName: String): ScopedFact {
   return ScopedFact(
      scope = ProjectionFunctionScope(scopeName, fact.type.taxiType),
      fact = fact
   )
}
