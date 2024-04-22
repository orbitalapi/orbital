package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.StubService
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyneWithStub

abstract class BaseHazelcastInvokerTest {
   open fun defaultSchema(): String = """
         import com.orbitalhq.hazelcast.HazelcastService
         import com.orbitalhq.hazelcast.HazelcastMap
         import com.orbitalhq.hazelcast.UpsertOperation
         import com.orbitalhq.hazelcast.CompactObject
         model Person {
            name : PersonName inherits String
         }
         type Language inherits String

         @HazelcastMap(name = "films")
         @CompactObject
         closed model Film {
            @Id
            filmId : FilmId inherits Int
            title : Title inherits String
            languages: Language[]
            director : Person
            cast : Person[]
         }
         @HazelcastService(connectionName = "notUsed")
         service HazelcastService {
            @UpsertOperation
            write operation upsert(Film):Film

            table films : Film[]

            stream films : Stream<Film>
         }
         """
   fun vyneWithHazelcast(schema: String = defaultSchema()):Triple<HazelcastInstance, Vyne, StubService> {
      val hazelcastInstance = TestHazelcastInstanceFactory(1).newHazelcastInstance()
      val hazelcastProvider = TestHazelcastProvider(hazelcastInstance)
      val hazelcastInvoker = HazelcastInvoker(hazelcastProvider)
      val (vyne, stub) = testVyneWithStub(
         TaxiSchema.fromStrings(
            listOf(
               VyneQlGrammar.QUERY_TYPE_TAXI,
               HazelcastTaxi.schema,
               schema,
            )
         ), listOf(hazelcastInvoker)
      )
      return Triple(hazelcastInstance, vyne, stub)
   }
}
