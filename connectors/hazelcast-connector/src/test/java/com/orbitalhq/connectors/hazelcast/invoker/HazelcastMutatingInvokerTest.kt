package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.internal.serialization.impl.compact.DeserializedGenericRecord
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyneWithStub
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.types.ObjectType
import org.junit.jupiter.api.Test

class HazelcastMutatingInvokerTest : BaseHazelcastInvokerTest() {
   @Test
   fun `can write portable object`():Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      vyne.query("""given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call HazelcastService::upsert""".trimMargin())
         .firstRawObject()
      val filmMap = hazelcastInstance.getMap<Int,Any>("films")
      filmMap.shouldNotBeNull()
      val film = filmMap.get(100)
      film.shouldNotBeNull()
      film.shouldBeInstanceOf<DeserializedGenericRecord>()

      val typedInstance = GenericRecordReader.toTypedInstance(film, vyne.type("Film").taxiType as ObjectType, vyne.schema, UndefinedSource)
      val rawValue = typedInstance.toRawObject()
      rawValue.shouldBe(mapOf(
         "filmId" to 100,
         "title" to "Star Wars",
         "languages" to listOf("English", "American"),
         "director" to mapOf("name" to "George"),
         "cast" to listOf(mapOf("name" to "Mark"), mapOf("name" to "Carrie"))
      ))

   }

   @Test
   fun `can read and write multiple objects`():Unit = runBlocking {
      val schema = """
      import com.orbitalhq.hazelcast.HazelcastService
      import com.orbitalhq.hazelcast.HazelcastMap
      import com.orbitalhq.hazelcast.UpsertOperation
      import com.orbitalhq.hazelcast.CompactObject

      type FilmId inherits Int
      type Title inherits String

      closed model ApiFilm {
         @Id
         filmId : FilmId
         title : Title
      }

      service ApiService {
         operation getFilms():ApiFilm[]
      }

      @CompactObject
      @HazelcastMap(name = "films")
      closed parameter model HzcFilm {
         @Id
         id : FilmId
         name : Title
      }
      @HazelcastService(connectionName = "notUsed")
      service HazelcastService {
         @UpsertOperation
         write operation upsert(HzcFilm):HzcFilm

         @DeleteOperation(mapName = "films")
         write operation deleteAll()

         @DeleteOperation
         write operation deleteSingle(HzcFilm):HzcFilm

         @DeleteOperation
         write operation deleteByKey(FilmId):HzcFilm

         table films : HzcFilm[]
      }

      """.trimIndent()
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast(schema)
      stub.addResponse("getFilms", vyne.parseJson("ApiFilm[]", """
         [
            { "filmId" : 100, "title" : "Star Wars" },
            { "filmId" : 200, "title" : "Empire Strikes Back" },
            { "filmId" : 300, "title" : "Return of the Jedi" }
         ]
      """.trimIndent()))

      val upsertResult = vyne.query("""
         find { ApiFilm[] }
         call HazelcastService::upsert
//         excluding { HazelcastService::films }
      """.trimIndent())
         .rawObjects()
      upsertResult.shouldHaveSize(3)

      val findResult = vyne.query("""find { HzcFilm[] }""")
         .rawObjects()
      findResult.shouldHaveSize(3)
   }


}
