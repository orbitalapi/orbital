package io.vyne

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.asA
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * These tests focus on the usage of the single() filter operation
 */
class CollectionFilteringSingleTest {
   val schema = TaxiSchema.from(
      """
       model Person {
           id : PersonId inherits Int
           name : PersonName inherits String
          }
          model Movie {
            cast : Person[]
         }
          service PersonService {
            operation getAll():Movie[]
         }
   """.trimIndent()
   )
   val movieJson = """[{
         | "cast" : [
         | { "id" : 1, "name" : "Jack" },
         | { "id" : 2, "name" : "Sparrow" },
         | { "id" : 3, "name" : "Butcher" }
         |]
         |}]
   """.trimMargin()


   @Test
   fun `can filter a list of types to a single from a property on the projected type`():Unit = runBlocking{
      jacksonObjectMapper().readValue<Map<String,Any>>("")
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query("""find { Movie[] } as {
         | cast : Person[]
         | // Filtering directly on a field on this type.
         | starring : single(this.cast, (Person) -> PersonId == 1 )
         |}[]
      """.trimMargin())
         .typedObjects()
      val movie = results.single().asA<TypedObject>()
      val starring = movie.get("starring").toRawObject()
      starring.should.equal(mapOf("id" to 1, "name" to "Jack"))
   }

   @Test
   fun `can filter a list of types to a single from a type reference against the source type`():Unit = runBlocking{
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query("""find { Movie[] } as {
         | // Person[] is a field on the Movie[] type, but isn't
         | // selected into the projection.
         | // This requires different search approach
         | starring : single(Person[], (Person) -> PersonId == 1 )
         |}[]
      """.trimMargin())
         .typedObjects()
      val movie = results.single().asA<TypedObject>()
      val starring = movie.get("starring").toRawObject()
      starring.should.equal(mapOf("id" to 1, "name" to "Jack"))
   }
   @Test
   fun `can filter a list of types to a single and project result`():Unit = runBlocking{
      val (vyne,stub) = testVyne(schema)
      stub.addResponse("getAll", vyne.parseJson("Movie[]", movieJson))
      val results = vyne.query("""find { Movie[] } as {
         | cast : Person[]
         | starring : single(this.cast, (Person) -> PersonId == 1 ) as {
         |    name : PersonName
         | }
         |}[]
      """.trimMargin())
         .typedObjects()
      val movie = results.single().asA<TypedObject>()
      val starring = movie.get("starring").toRawObject()
      starring.should.equal(mapOf("name" to "Jack"))
   }


}
