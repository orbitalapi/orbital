package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * These are tests that explore / assert how we traverse
 * collection relationships when enriching / projecting within a query
 */
class VyneCollectionDiscoveryTest {
   @Test
   fun `given an array of discovered values, ids present in those arrays can look up other values`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type ActorId inherits Int
         model Actor {
            @Id
            id : ActorId
            name : ActorName inherits String
         }
         model Cast {
            actors : ActorId[]
         }
         model Movie {
            title : MovieTitle inherits String
            cast : Cast
         }
         service MovieService {
            operation findAllMovies():Movie[]
            operation findActor(ActorId):Actor
         }

         model OutputMovie {
            movieTitle: MovieTitle
            actors: Actor[]
         }
      """
      )
      val movies = TypedInstance.from(
         vyne.type("Movie"), """
         [ { "cast" : { "actors" : [1,2,3] } } ]
      """.trimIndent(), vyne.schema, source = Provided
      )
      val actors = mapOf(
         1 to mapOf("id" to 1, "name" to "Mickey Mouse"),
         2 to mapOf("id" to 2, "name" to "Donald Duck"),
         3 to mapOf("id" to 3, "name" to "Scrooge McDuck")
      ).mapValues { (_, value) -> TypedInstance.from(vyne.type("Actor"), value, vyne.schema, source = Provided) }
      stub.addResponse("findAllMovies", movies)
      stub.addResponse("findActor") { remoteOperation, parameters ->
         val actorId = parameters[0].second.value as Int
         listOf(actors[actorId]!!)
      }

      val results = vyne.query("""findAll { Movie[] } as OutputMovie[]""").typedObjects()
      results.should.have.size(1)
      val result = results.first()
      TODO()
   }
}
