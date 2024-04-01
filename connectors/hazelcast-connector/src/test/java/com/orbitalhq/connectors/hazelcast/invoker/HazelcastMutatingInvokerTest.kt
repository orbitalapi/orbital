package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.internal.serialization.impl.compact.DeserializedGenericRecord
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyneWithStub
import io.kotest.common.runBlocking
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

}
