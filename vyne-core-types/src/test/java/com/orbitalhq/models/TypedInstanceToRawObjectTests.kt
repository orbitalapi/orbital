package com.orbitalhq.models

import com.orbitalhq.models.facts.CopyOnWriteFactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test

class TypedInstanceToRawObjectTests {
    val schema = TaxiSchema.from(

        """
      @com.orbitalhq.models.OmitNulls
      model Person {
         id : PersonId inherits Int   
         name : FirstName? inherits String
      }
      
      @com.orbitalhq.models.OmitNulls
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
    fun `toRawObject should exclude keys with null values when model is annotate with OmitNulls`() {
        val person = TypedInstance.from(schema.type("Person"), """ {  "id" : 1 }""", schema)
        val factBag = CopyOnWriteFactBag(listOf(person), schema)
        val value =
            factBag.getFact(schema.type("Person"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)

        val map =  value.toRawObject() as Map<String, Any?>
        map.containsKey("name").should.be.`false`
    }

    @Test
    fun `OmitNulls should be applied for nested conversions`() {
        val catalog = TypedInstance.from(
            schema.type("Catalog"), """
            {
               "films" : [
                  { "cast" : [
                     { "id": 1, "name" : null,  "agentName" : "Jenny" },
                     { "id": 2, "name" : "Carrie" , "agentName" : "Amanda" }
                    ]
                  },
                  { "cast" : [
                     { "id":3, "name" : "George" , "agentName" : "Sophie" },
                     { "id":4, "name" : "Hamish" , "agentName" : "Leslie" }
                    ]
                  }
               ]
            }
         """.trimIndent(), schema
        )

        val factBag = CopyOnWriteFactBag(catalog, schema)
        val value =
            factBag.getFact(schema.type("Catalog"), FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)

        val map =  value.toRawObject() as Map<String, Any?>
        val firstFilm = (map["films"] as List<Map<String, Any?>>)[0]
        val firstFilmCasts = firstFilm["cast"] as List<Map<String, Any?>>

        // First Cast has a null name, so we shouldn't get name property as Actor Type has @OmitNull
        firstFilmCasts[0].containsKey("name").should.be.`false`
        // Whereas the second cast has a name
        firstFilmCasts[1].containsKey("name").should.be.`true`
    }
}