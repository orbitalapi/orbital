package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CollectionFilteringSingleByTest {
   val schema = TaxiSchema.from(
      """
         type UnionId inherits String
         model PersonIds {
            agencyId : AgencyId inherits String
            imdbId : ImdbId inherits String
            unionIds : UnionId[]
         }
         // For our test, these are the subset of ids we want to match on
         model MatchingIds {
            agencyId : AgencyId inherits String
            unionIds : UnionId[]
         }
       model MovieAwardList {
         movies : Movie[]
         awards : Award[]
       }
       model Award {
         person : PersonIds
         awardTitle : AwardTitle inherits String
       }
       model Person {
           name : PersonName inherits String
           ids : PersonIds
          }
          model Movie {
            title : MovieTitle inherits String
            cast : Person[]
         }
          service MoviesService {
            operation getAll():MovieAwardList
         }
   """.trimIndent()
   )
   val movieJson = """{
  "movies": [
    {
      "title": "Pirates",
      "cast": [
        {
          "name": "Depp",
          "ids": {
            "agencyId": "A1",
            "imdbId": "I1",
            "unionIds": [ "U1" ]
          }
        },
        {
          "name": "Jill",
          "ids": {
            "agencyId": "A2",
            "imdbId": "I2",
            "unionIds": [ "U2"]
          }
        }
      ]
    }
  ],
  "awards": [
    {
      "person": {
        "agencyId": "A1",
        "imdbId": null,
        "unionIds": ["U1"]
      },
      "awardTitle": "Best Actor"
    }
  ]
}
   """.trimMargin()

   @Test
   fun `can filter a collection using singleBy using a projected value`():Unit = runBlocking {
      val (vyne,stub) = testVyne(schema)
      val movies = vyne.parseJson("MovieAwardList", movieJson)
      stub.addResponse("getAll", movies)
      val queryResult = vyne.query("""
         find { MovieAwardList } as {
            title : MovieTitle
            cast : Person[] as (person:Person) -> {
               name: PersonName
               award : singleBy(Award[], (Award) -> Award::PersonIds as MatchingIds , person.ids as MatchingIds) as AwardTitle
            }[]
         }

      """.trimIndent()).firstTypedObject()
      queryResult.shouldNotBeNull()
      queryResult.toRawObject().shouldBe(mapOf(
         "title" to "Pirates",
         "cast" to listOf(
            mapOf("name" to "Depp", "award" to "Best Actor"),
            mapOf("name" to "Jill", "award" to null),

            )
      ))

   }

   @Test
   fun `can filter a collection using singleBy using a converted value`():Unit = runBlocking {
      // Rather than using projections, this uses the convert() function, which is
      // less powerful, but more performant.
      // The results should be the same as the above test
      val (vyne,stub) = testVyne(schema)
      val movies = vyne.parseJson("MovieAwardList", movieJson)
      stub.addResponse("getAll", movies)
      val queryResult = vyne.query("""
         find { MovieAwardList } as {
            title : MovieTitle
            cast : Person[] as (person:Person) -> {
               name: PersonName
               award : singleBy(Award[], (Award) -> convert(Award::PersonIds, MatchingIds) , convert(person.ids, MatchingIds)) as AwardTitle
            }[]
         }

      """.trimIndent()).firstTypedObject()
      queryResult.shouldNotBeNull()
      queryResult.toRawObject().shouldBe(mapOf(
         "title" to "Pirates",
         "cast" to listOf(
            mapOf("name" to "Depp", "award" to "Best Actor"),
            mapOf("name" to "Jill", "award" to null),

            )
      ))

   }


}
