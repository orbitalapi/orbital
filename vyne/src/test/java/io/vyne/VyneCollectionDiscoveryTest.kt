package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * These are tests that explore / assert how we traverse
 * collection relationships when enriching / projecting within a query
 */
class VyneCollectionDiscoveryTest {

   @Test
   fun `when a service returns a collection we can use the child attributes to populate an input to another service`() : Unit = runBlocking {
      // This test is about:
      // Person -[has]-> Name
      // operation foo():Person[]
      // operation bar(Name[]):Something[]
      // We should be able to map Person[] -> Name[] to invoke operation bar, and find a Something[]
      val (vyne,stub) = testVyne("""
         model Person {
            id : PersonId as Int
            townId : TownId as Int
         }
         model Town {
            id : TownId
         }
         service Foo {
            operation findAllFriends(PersonId):Person[]
            operation findAllTowns(TownId[]):Town[]
         }
      """.trimIndent())
      stub.addResponse("findAllFriends", vyne.parseJson("Person[]", """[{ "id": 1, "townId" : 100 }, {"id" : 2, "townId": 200 }] """))
      stub.addResponse("findAllTowns", vyne.parseJson("Town[]", """[{ "id": 100}, {"id" : 200} ] """))
      val result = vyne.from(vyne.parseKeyValuePair("PersonId", 1)).find("Town[]")
         .typedInstances()
      result.should.have.size(2)
      val invocations = stub.invocations["findAllTowns"]!!
      val paramsPassedToStub = (invocations[0] as TypedCollection).map { it.value }
      paramsPassedToStub.should.equal(listOf(100,200))
   }

   @Test
   fun `given an array of discovered values, ids present in those arrays can look up attributes from other types`(): Unit =
      runBlocking {
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
            actors: ActorName[]
         }
      """
         )
         val movies = TypedInstance.from(
            vyne.type("Movie"), """
         [ { "title" : "The ducks take Manhattan", "cast" : { "actors" : [1,2,3] } } ]
      """.trimIndent(), vyne.schema, source = Provided
         )
         stub.addResponse("findAllMovies", movies)

         val actors = mapOf(
            1 to mapOf("id" to 1, "name" to "Mickey Mouse"),
            2 to mapOf("id" to 2, "name" to "Donald Duck"),
            3 to mapOf("id" to 3, "name" to "Scrooge McDuck")
         ).mapValues { (_, value) -> TypedInstance.from(vyne.type("Actor"), value, vyne.schema, source = Provided) }

         stub.addResponse("findActor") { remoteOperation, parameters ->
            val actorId = parameters[0].second.value as Int
            listOf(actors[actorId]!!)
         }

         val results = vyne.query("""findAll { Movie[] } as OutputMovie[]""").typedObjects()
         results.should.have.size(1)
         val result = results.first().toRawObject()
         result.should.equal(
            mapOf(
               "movieTitle" to "The ducks take Manhattan",
               "actors" to listOf(
                  "Mickey Mouse",
                  "Donald Duck",
                  "Scrooge McDuck"
               ),
            )
         )
      }


   @Test
   fun `given an array of discovered values, ids present in those arrays can build anonymous types from other types`(): Unit =
      runBlocking {
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
            actors: ActorName[]
         }
      """
         )
         val movies = TypedInstance.from(
            vyne.type("Movie"), """
         [ { "title" : "The ducks take Manhattan", "cast" : { "actors" : [1,2,3] } } ]
      """.trimIndent(), vyne.schema, source = Provided
         )
         stub.addResponse("findAllMovies", movies)

         val actors = mapOf(
            1 to mapOf("id" to 1, "name" to "Mickey Mouse"),
            2 to mapOf("id" to 2, "name" to "Donald Duck"),
            3 to mapOf("id" to 3, "name" to "Scrooge McDuck")
         ).mapValues { (_, value) -> TypedInstance.from(vyne.type("Actor"), value, vyne.schema, source = Provided) }

         stub.addResponse("findActor") { remoteOperation, parameters ->
            val actorId = parameters[0].second.value as Int
            listOf(actors[actorId]!!)
         }

         val results = vyne.query(
            """findAll { Movie[] } as {
            | movieTitle: MovieTitle
            | actors: ActorName[]
            | }[]
         """.trimMargin()
         ).typedObjects()
         results.should.have.size(1)
         val result = results.first().toRawObject()
         result.should.equal(
            mapOf(
               "movieTitle" to "The ducks take Manhattan",
               "actors" to listOf(
                  "Mickey Mouse",
                  "Donald Duck",
                  "Scrooge McDuck"
               ),
            )
         )
      }


   @Test
   fun `given an array of discovered values, ids present in those arrays can look up other values`(): Unit =
      runBlocking {
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
         [ { "title" : "The ducks take Manhattan", "cast" : { "actors" : [1,2,3] } } ]
      """.trimIndent(), vyne.schema, source = Provided
         )
         stub.addResponse("findAllMovies", movies)

         val actors = mapOf(
            1 to mapOf("id" to 1, "name" to "Mickey Mouse"),
            2 to mapOf("id" to 2, "name" to "Donald Duck"),
            3 to mapOf("id" to 3, "name" to "Scrooge McDuck")
         ).mapValues { (_, value) -> TypedInstance.from(vyne.type("Actor"), value, vyne.schema, source = Provided) }

         stub.addResponse("findActor") { remoteOperation, parameters ->
            val actorId = parameters[0].second.value as Int
            listOf(actors[actorId]!!)
         }

         val results = vyne.query("""findAll { Movie[] } as OutputMovie[]""").typedObjects()
         results.should.have.size(1)
         val result = results.first().toRawObject()
         result.should.equal(
            mapOf(
               "movieTitle" to "The ducks take Manhattan",
               "actors" to listOf(
                  mapOf("id" to 1, "name" to "Mickey Mouse"),
                  mapOf("id" to 2, "name" to "Donald Duck"),
                  mapOf("id" to 3, "name" to "Scrooge McDuck"),
               )
            )
         )
      }

   @Test
   fun `can use projection scope to build nested collections`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Product {
             @Id
             sku : ProductSku inherits String
             baseSKU : BaseSKU inherits String
             size : ProductSize inherits String
         }
         model TransactionProduct {
             sku: ProductSku
         }
         model OrderTransaction {
             products: TransactionProduct[]
         }
         service Service {
             operation listOrders():OrderTransaction[]
             operation findProduct(@PathVariable(name="sku") sku:ProductSku):Product
         }
      """
      )
      stub.addResponse(
         "listOrders", vyne.parseJson(
            "OrderTransaction[]",
            """[
        { "products" : [ { "sku" : "TShirt-Small" } , { "sku" : "Hoodie-Small" } ] },
        { "products" : [ { "sku" : "TShirt-Large" } , { "sku" : "Hoodie-Large" } ] }
      ]""".trimIndent()
         )
      )
      stub.addResponse("findProduct") { _, params ->
         val (_, productSkuInstance) = params.first()
         val productSku = productSkuInstance.value as String
         val parts = productSku.split("-")
         listOf(
            TypedInstance.from(
               vyne.type("Product"), mapOf(
                  "sku" to productSku!!,
                  "baseSku" to parts[0],
                  "size" to parts[1]
               ), vyne.schema
            )
         )
      }
      val queryResult = vyne.query("""findAll { OrderTransaction[] } as {
         | items by [TransactionProduct] -> { sku: ProductSku size: ProductSize }[]
         | }[]
      """.trimMargin())
         .rawObjects()
      queryResult.size.should.equal(2)
      queryResult.should.equal(listOf(
         mapOf("items" to listOf(
            mapOf("sku" to "TShirt-Small", "size" to "Small"),
            mapOf("sku" to "Hoodie-Small", "size" to "Small"),
         )),
         mapOf("items" to listOf(
            mapOf("sku" to "TShirt-Large", "size" to "Large"),
            mapOf("sku" to "Hoodie-Large", "size" to "Large"),
         ))

      ))
   }

   @Test
   fun `given a nested source array, can project to an array of a different subtype of the same base type`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         type ActorId inherits String
         type Actor

         model ImdbMovie {
            title : MovieTitle inherits String
            cast : ImdbActor[]
         }
         model ImdbActor inherits Actor {
            id : ActorId
            name : ActorName inherits String
         }

         model RottenTomatoesMovie {
            filmName : MovieTitle
            actors : RottenTomatoesActor[]
         }
         model RottenTomatoesActor inherits Actor {
            actorName : ActorName
         }
         service MovieService {
            operation findAllMovies():ImdbMovie[]
         }
      """
         )
         val movies = TypedInstance.from(
            vyne.type("ImdbMovie"), """
         [ {
         "title" : "The ducks take Manhattan",
         "cast" : [
            { "id" : "duck-1", "name" : "Donald Duck" },
            { "id" : "duck-2", "name" : "Uncle Scrooge" }
         ]
         } ]
      """.trimIndent(), vyne.schema, source = Provided
         )
         stub.addResponse("findAllMovies", movies)

         val results = vyne.query("""findAll { ImdbMovie[] } as RottenTomatoesMovie[]""").typedObjects()
         results.should.have.size(1)
         val result = results.first().toRawObject()
         result.should.equal(
            mapOf(
               "filmName" to "The ducks take Manhattan",
               "actors" to listOf(
                  mapOf("actorName" to "Donald Duck"),
                  mapOf("actorName" to "Uncle Scrooge"),
               )
            )
         )
      }
}
