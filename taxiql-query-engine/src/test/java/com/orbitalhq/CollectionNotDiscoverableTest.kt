package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.NotFoundException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.assertFails

/**
 * This test outlines the expected behaviour if we cannot
 * find a collection, preferring returning an empty collection, rather
 * than failing.
 *
 * See https://projects.notional.uk/youtrack/issue/ORB-236 for discussion
 */
class CollectionNotDiscoverableTest : DescribeSpec({

   describe("handling collection unhappy paths") {
      describe("Fetching an array fails") {
         val (vyne,stub) = testVyne("""
            model Film {
               id : FilmId inherits Int
            }
            model Actor {
               name : ActorName inherits String
            }
            service Films {
               operation getFilms():Film[]
               operation getActors(FilmId):Actor[] // <--- in this test, the operation directly returns the actors
            }
         """.trimIndent())
         stub.addResponse("getFilms", vyne.parseJson("Film[]", """[ {"id" : 1} ]"""))
         it("should be an empty array by default") {
            stub.addResponseThrowing("getActors", NotFoundException("No actors here"))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to emptyList<Map<String,Any>>()
            ))
         }
         it("should be null if the array type is nullable") {
            stub.addResponseThrowing("getActors", NotFoundException("No actors here"))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]?
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to null
            ))
         }
      }
      describe("Fetching an object containing an array fails") {
         val (vyne,stub) = testVyne("""
            model Film {
               id : FilmId inherits Int
            }
            model Actor {
               name : ActorName inherits String
            }
            model Cast {
               actors: Actor[]
            }
            service Films {
               operation getFilms():Film[]
               operation getCast(FilmId):Cast // <--- in this test, the operation directly returns an object containing the list of actors
            }
         """.trimIndent())
         stub.addResponse("getFilms", vyne.parseJson("Film[]", """[ {"id" : 1} ]"""))
         it("should be an empty array by default") {
            stub.addResponseThrowing("getActors", NotFoundException("No actors here"))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to emptyList<Map<String,Any>>()
            ))
         }
         it("should be null if the array type is nullable") {
            stub.addResponseThrowing("getActors", NotFoundException("No actors here"))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]?
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to null
            ))
         }
      }
      describe("Array on a response object is served as null or missing") {
         val (vyne,stub) = testVyne("""
            model Film {
               id : FilmId inherits Int
            }
            closed model Actor {
               name : ActorName inherits String
            }
            closed model Cast {
               actors: Actor[]?
            }
            service Films {
               operation getFilms():Film[]
               operation getCast(FilmId):Cast // <--- in this test, the operation directly returns an object containing the list of actors
            }
         """.trimIndent())
         stub.addResponse("getFilms", vyne.parseJson("Film[]", """[ {"id" : 1} ]"""))

         // Excluded, should be implemented as part of ORB-237
         xit("should fail if the served value is null and the query contract is not nullable") {
            stub.addResponse("getCast", vyne.parseJson("Cast", """{ "actors" : null }"""))
            assertFails {
               val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]
               }[]
            """.trimIndent())
               .firstRawObject()
               // You shouldn't hit this line.
               result.shouldBeNull()
            }

         }
         // Excluded, should be implemented as part of ORB-237
         xit("should fail if the served value is missing and the query contract is not nullable") {
            stub.addResponse("getCast", vyne.parseJson("Cast", """{}"""))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]
               }[]
            """.trimIndent())
               .firstRawObject()
            assertFails { vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]
               }[]
            """.trimIndent())
               .firstRawObject()
            }
         }
         it("should be null if the array type is served as null and the array type is nullable") {
            stub.addResponse("getCast", vyne.parseJson("Cast", """{ "actors" : null }"""))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]?
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to null
            ))
         }
         it("should be null if the array type is not present and the array type is nullable") {
            stub.addResponse("getCast", vyne.parseJson("Cast", """{ }"""))
            val result = vyne.query("""
               find { Film[] } as {
                  id : FilmId
                  cast : Actor[]?
               }[]
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf(
               "id" to 1,
               "cast" to null
            ))
         }
      }
   }
})
