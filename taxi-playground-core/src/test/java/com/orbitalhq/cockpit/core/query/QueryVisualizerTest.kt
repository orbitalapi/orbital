package com.orbitalhq.cockpit.core.query

import arrow.core.getOrElse
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.history.QueryPlanDiagramData
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import lang.taxi.annotations.HttpOperation
import org.http4k.quoted

class QueryVisualizerTest : DescribeSpec({

   describe("Query visualizer") {
      val visualizer = QueryVisualizer()
      it("should generate a diagram json for an executed query") {
         val schema = TaxiSchema.from(
            """
            model Person {
               name : PersonName inherits String
               age : PersonAge inherits Int
            }
            service PersonService {
               operation getPerson(emailAddress: String):Person
            }
         """.trimIndent()
         )
         val query = """
            import taxi.stdlib.upperCase
            given {email: String = "jimmy@foo.com"}
            find {
                    hello: String = 'World',
                // Accessing properties by field name
                name : PersonName = PersonService::getPerson(email).name.upperCase()
                // Accessing properties by type
                age : PersonAge = PersonService::getPerson(email)::PersonAge
            }
         """.trimIndent()
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }
         data.shouldHaveLinks("ProvidedInput/lang.taxi.String/jimmy@foo.com" to "PersonService@@getPerson::emailAddress",
            "PersonService@@getPerson" to "Person",
            "ProvidedInput/lang.taxi.String/World" to "Anon::hello",
            "Person::name" to "bytaxi.stdlide8fee05058a8d2f3fb555de181c7cb8",
            "bytaxi.stdlide8fee05058a8d2f3fb555de181c7cb8" to "Anon::name",
            "Person::age" to "Anon::age")

      }

      // Mutations aren't working yet
      xit("should generate a diagram a query with a mutation") {
         val schema = TaxiSchema.from(
            """
            closed model Person {
               name : PersonName inherits String
               age : PersonAge inherits Int
               email : EmailAddress inherits String
            }

            parameter model UpdatePersonRequest {
               name : PersonName
//               increasedAge : UpdatedAge inherits Int
            }

            service PersonService {
               operation getPerson(emailAddress: EmailAddress):Person
               write operation updatePerson(request:UpdatePersonRequest):UpdatePersonResponse
            }

            closed model UpdatePersonResponse {
               result : Result inherits String
            }
         """.trimIndent()
         )
         val query = """
            import taxi.stdlib.upperCase
            given {email: EmailAddress = "jimmy@foo.com"}
            find {
                name : PersonName //= (PersonName) PersonService::getPerson(email).name.upperCase()
                // Accessing properties by type
                age : PersonAge = PersonService::getPerson(email)::PersonAge
            } /*as {
               updated: UpdatedAge = PersonAge // + 1
            }*/
            call PersonService::updatePerson
         """.trimIndent()
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }
         data.shouldHaveLinks()

      }

      it("should generate for a streaming query") {
         val schema = TaxiSchema.from(
            """
         ${VyneQlGrammar.QUERY_TYPE_TAXI}
         namespace foo {
            model Film {
               id : FilmId inherits Int
            }
            model Review {
               score : ReviewScore inherits Int
            }
           @KafkaService( connectionName = "moviesConnection" )
            service FilmsTopic {
            @KafkaOperation( topic = "someTopic", offset = "earliest" )
               stream films : Stream<Film>
            }
            service ReviewsApi {
               @${HttpOperation.NAME}(method = "GET", url="http://fakeUrl")
               operation getReview(FilmId):Review
            }
         }
      """.trimIndent()
         )
         val query = """stream { foo.Film } as {
         id : foo.FilmId
         score : foo.ReviewScore
      }[]
      """
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }
         data.shouldHaveLinks("foo.FilmsTopic@@films" to "foo.Film",
            "foo.Film::id" to "foo.ReviewsApi@@getReview::p0",
            "foo.ReviewsApi@@getReview" to "foo.Review",
            "foo.Film::id" to "Anon.Anon::id",
            "foo.Review::score" to "Anon.Anon::score")
      }

      it("should generate a diagram with a when clause") {
         val schema = TaxiSchema.from(
            """
            model OrderDetails {
               quantity : OrderedQuantity inherits Int
               delivered : DeliveredQuantity inherits Int
            }

            service OrderApi {
               operation getOrder():OrderDetails
            }
         """.trimIndent()
         )

         val query = """
            find { OrderDetails } as {
                delta: String = when {
                    OrderedQuantity == DeliveredQuantity -> "Just right"
                    else -> "Oops"
                }
            }
         """.trimIndent()
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }
         data.shouldHaveLinks("OrderApi@@getOrder" to "OrderDetails",
            "OrderDetails::quantity" to "when-case-0",
            "OrderDetails::delivered" to "when-case-0",
            "when{Ordered82a20cea85d9da842a68470ebc0c4ad7" to "Anon.Anon::delta")
      }
      it("should generate a diagram for a query with input objects") {
         val schema = TaxiSchema.from(
            """import ReviewId
import FilmId
parameter model ReviewRequest {
   id : ReviewId inherits String
}

model Film {
   id : FilmId inherits String
   title : Title inherits String
}

model Review {
   score: ReviewScore inherits Int
}

model IdLookup {
      filmId : FilmId
      reviewId : ReviewId
}
service Api {
      operation getFilm(FilmId):Film
      operation getReview(ReviewRequest):Review
      operation getIds(FilmId):IdLookup
}
"""
         )
         val query = """
given { FilmId = "1"}
find { score : ReviewScore }
"""
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }
         data.shouldHaveLinks("ProvidedInput/FilmId/1" to "Api@@getIds::p0",
            "Api@@getIds" to "IdLookup",
            "IdLookup::reviewId" to "ReviewRequest::id",
            "ReviewRequest" to "Api@@getReview::p0",
            "Api@@getReview" to "Review",
            "Review::score" to "Anon::score")
      }

      it("should generate a diagram for a joined stream") {
         val schema = TaxiSchema.from(
            """
            model FilmA {
               filmId : FilmId inherits Int
               reviewId : ReviewId inherits Int
            }
            model FilmB {
               movieId : MovieId inherits Int
            }
            model ReviewsLookup {
               filmId : FilmId
               reviewId : ReviewId
            }
            model Review {
               score : ReviewScore inherits Int
            }
            service FilmsAndReviews {
               stream filmA: Stream<FilmA>
               stream filmB: Stream<FilmB>
               operation getReviews(ReviewId):Review
               operation getLookup(MovieId):ReviewsLookup
            }
         """.trimIndent()
         )

         val query = """
            stream { FilmA | FilmB } as {
               id: FilmId
               score : ReviewScore
            }[]
         """.trimMargin()
         val data = visualizer.generateQueryDiagram(query, schema)
            .getOrElse { throw (it) }

         data.shouldHaveLinks("FilmsAndReviews@@filmA" to "FilmA",
            "FilmsAndReviews@@filmB" to "FilmB",
            "FilmB::movieId" to "FilmsAndReviews@@getLookup::p0",
            "FilmA::reviewId" to "FilmsAndReviews@@getReviews::p0",
            "FilmsAndReviews@@getLookup" to "ReviewsLookup",
            "FilmsAndReviews@@getReviews" to "Review",
            "ReviewsLookup::reviewId" to "FilmsAndReviews@@getReviews::p0",
            "FilmA::filmId" to "Anon.Anon::id",
            "Review::score" to "Anon.Anon::score",
            "ReviewsLookup::filmId" to "Anon.Anon::id")
      }
   }

})

private fun QueryPlanDiagramData.shouldHaveLinks(vararg links: Pair<String, String>) {
   fun makeIdStable(value: String): String = value.replace(Regex("AnonymousType[a-zA-Z0-9_]+"), "Anon")
      .let { if (it.startsWith("when-")) {
         "when-case-" + it.substringAfter("-case-")
      } else it }
   fun asLoggableString(source: String, target: String) = "${source.quoted()} to ${target.quoted()}"
   val missing = mutableListOf<String>()
   links.forEach { (source, target) ->
      if (!this.links.any { link ->
         makeIdStable(link.sourceHandleId) == source && makeIdStable(link.targetHandleId) == target
      }) missing.add(asLoggableString(source,target))
   }

   val actualDescription = this.links.joinToString(",\n") { asLoggableString(makeIdStable(it.sourceHandleId), makeIdStable(it.targetHandleId)) }
   val actualPlan = this
   if (missing.isNotEmpty() || links.size != this.links.size) {
      val failure = buildString {
         append("Query plan did not have expected links.")
         if (links.size != actualPlan.links.size) {
            append(" Expected ${links.size} links, but ${actualPlan.links.size} were present")
         }
         if (missing.isNotEmpty()) {
            appendLine(" The following links were missing: \n${missing.joinToString("\n")} ")
         }
         appendLine("Links present were:")
         append(actualDescription)
      }
      fail(failure)
   }
}

