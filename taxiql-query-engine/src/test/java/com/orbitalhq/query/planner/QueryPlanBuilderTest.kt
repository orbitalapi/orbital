package com.orbitalhq.query.planner

import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize

class QueryPlanBuilderTest : DescribeSpec({

   describe("building query plan metadata") {
      it("should list all fields and their possible return types") {
         val schema = TaxiSchema.from("""
            model Film {
               title : FilmTitle inherits String
               id : FilmId inherits Int
            }
            model Actor {
               name : PersonName inherits String
            }
            service FilmService {
               operation getFilms():Film[]
               operation getCast(FilmId):Actor[]
            }
         """.trimIndent())
         val (query,_,querySchema) = schema.parseQuery(
             """find { Film[] } as {
                |name : FilmTitle
                |cast : Actor[] as {
                |  castMemberName : PersonName
                |}[]
                |}[]""".trimMargin()
         )
         val metadata = QueryPlanner().buildMetadata(query, querySchema)
         metadata.typesAndCandidateSources.keys.map { it.type.name.shortDisplayName }
            .shouldContainAll("FilmTitle", "Actor[]", "PersonName", "Film[]")
         metadata.allCandidateOperations.shouldHaveSize(2)
      }
      it("should list all stream types") {
         val schema = TaxiSchema.from("""
            model Tweet {
               id : TweetId inherits String
               text : TweetText inherits String
            }
            model Analytics {
               viewCount : ViewCount inherits Int
            }
            service TweetServices {
               stream tweets:Stream<Tweet>
               stream analytics : Stream<Analytics>
            }
         """.trimIndent())
         val (query,_,querySchema) = schema.parseQuery(
             """stream { Tweet } as {
                | id : TweetId
                | text : TweetText
                | viewCount : ViewCount
                |}[]
             """.trimMargin()
         )
         val metadata = QueryPlanner().buildMetadata(query, querySchema)
         metadata.allCandidateOperations.shouldHaveSize(2)
         metadata.candidateStreamOperations.shouldHaveSize(2)
      }
   }
})
