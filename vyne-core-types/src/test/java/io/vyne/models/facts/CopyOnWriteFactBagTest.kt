package io.vyne.models.facts

import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldNotBeNull
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class CopyOnWriteFactBagTest {
   val schema = TaxiSchema.from(

      """
      model Person {
         id : PersonId inherits Int
         name : FirstName inherits String
      }
      model Actor inherits Person {
         agentName : AgentName inherits String
      }

      model Film {
        cast : Actor[]
        crew : Person[]
        imdbScore : ImdbScore inherits Decimal
      }

      model Catalog {
         films : Film[]
      }
   """.trimIndent()
   )

   @Test
   fun `can find a property on an object`() {
      val person = TypedInstance.from(schema.type("Person"), """ { "name" : "Jimmy", "id" : 1 }""", schema)
      val factBag = CopyOnWriteFactBag(listOf(person), schema)
      val value =
         factBag.getFact(schema.type("FirstName"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      value.toRawObject().should.equal("Jimmy")
   }

   @Test
   fun `can find a fact by type`() {
      val person = TypedInstance.from(schema.type("Person"), """ { "name" : "Jimmy" }""", schema)
      val actor = TypedInstance.from(schema.type("Actor"), """ { "name" : "Jack" }""", schema)
      val factBag = CopyOnWriteFactBag(listOf(person, actor), schema)

      val collection =
         factBag.getFact(schema.type("Person"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      collection.should.have.size(2)
   }


   @Test
   fun `can find a collection of properties from within a collection`() {
      val catalog = TypedInstance.from(
         schema.type("Catalog"), """
            {
               "films" : [
                  { "cast" : [
                     { "name" : "Mark" , "agentName" : "Jenny" },
                     { "name" : "Carrie" , "agentName" : "Amanda" }
                    ]
                  },
                  { "cast" : [
                     { "name" : "George" , "agentName" : "Sophie" },
                     { "name" : "Hamish" , "agentName" : "Leslie" }
                    ]
                  }
               ]
            }
         """.trimIndent(), schema
      )
      val factBag = CopyOnWriteFactBag(catalog, schema)
      val collection =
         factBag.getFact(schema.type("AgentName"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      collection.should.have.size(4)

   }

   @Test
   fun `two searches are considered equal`() {
      val person = TypedInstance.from(schema.type("Person"), """ { "name" : "Jimmy" }""", schema)
      val actor = TypedInstance.from(schema.type("Actor"), """ { "name" : "Jack" }""", schema)
      val factBag = CopyOnWriteFactBag(listOf(person, actor), schema)

      factBag.getFact(schema.type("Person"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      factBag.searchIsCached(schema.type("Person"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY).should.be.`true`
   }

   @Test
   fun `querying allow all for collections returns a flattened collection`() {
      val film = TypedInstance.from(
         schema.type("Film"), """{
            "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
            "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ]
            }""", schema
      )

      val factBag = CopyOnWriteFactBag(listOf(film), schema)

      val collection =
         factBag.getFact(schema.type("Person"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      // Result should be flattened - i.e.,
      // Expect a single collection with all elements, not a collection of two collections, with two elements each
      collection.should.have.size(4)
   }

   @Test
   fun `querying for a collection of types collects non collection values that match on type`() {
      val film = TypedInstance.from(
         schema.type("Film"), """{
            "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
            "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ],
            "imdbScore" : 5.5
            }""", schema
      )
      val factBag = CopyOnWriteFactBag(listOf(film), schema)
      val facts =
         factBag.getFact(schema.type("ImdbScore[]"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      facts.toRawObject().should.equal(listOf(5.5))
   }

   @Test
   fun `querying for a collection of types collects non collection values that match on type within nested collection`() {
      val film = TypedInstance.from(
         schema.type("Catalog"), """{
            "films" : [
            {
               "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
               "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ],
               "imdbScore" : 5.5
            },
            {
               "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
               "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ],
               "imdbScore" : 2.5
            }
         ]
         }""", schema
      )
      val factBag = CopyOnWriteFactBag(listOf(film), schema)
      val facts =
         factBag.getFact(schema.type("ImdbScore[]"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
      facts.toRawObject().should.equal(listOf(5.5, 2.5))
   }

   @Test
   fun `requesting a collection type returns the collection`() {
      val film = TypedInstance.from(
         schema.type("Catalog"), """{
            "films" : [
            {
               "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
               "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ],
               "imdbScore" : 5.5
            },
            {
               "cast": [   { "name" : "Jack" } , { "name" : "Jimmy" } ],
               "crew" : [ { "name" : "Pete" } , { "name" : "Paul" } ],
               "imdbScore" : 2.5
            }
         ]
         }""", schema
      )
      val factBag = CopyOnWriteFactBag(listOf(film), schema)
      val facts = factBag.getFact(schema.type("Film[]"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE) as TypedCollection
      facts.should.have.size(2)
   }

   @Test
   fun `can fetch an enum synonym from a factbag`() {
      val schema = TaxiSchema.from("""
         enum TwoLetterCountryCode {
            NZ,
            UK
         }
         enum ThreeLetterCountryCode {
            NZL synonym of TwoLetterCountryCode.NZ,
            UKI synonym of TwoLetterCountryCode.UK
         }
         model Person {
            country: TwoLetterCountryCode
         }
      """.trimIndent())
      val person = TypedInstance.from(schema.type("Person"), """{ "country": "NZ" }""", schema)
      val factBag = CopyOnWriteFactBag(person, schema)
      val result = factBag.getFact(schema.type("ThreeLetterCountryCode"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      result.shouldNotBeNull()
   }
}
