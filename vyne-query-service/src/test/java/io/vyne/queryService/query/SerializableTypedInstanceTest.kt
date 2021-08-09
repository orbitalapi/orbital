package io.vyne.queryService.query

import io.vyne.models.SerializableTypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.toSerializable
import io.vyne.testVyne
import io.vyne.utils.Benchmark
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.Test
import kotlin.test.assertEquals

class SerializableTypedInstanceTest {
   @Test
   fun `can convert type named instance to and from bytes`() {
      val (vyne, _) = testVyne("""
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
            fullName : FullName inherits String by concat(this.firstName, ' ', this.lastName)
         }
         model Author inherits Person
         model Book {
            author : Author
            reviewers : Person[]
            title : BookTitle inherits String
            subtitle: SubTitle inherits String
            releaseDate : ReleaseDate inherits Date
            imdbScore : ImdbScore inherits Decimal
            released : IsReleased inherits Boolean
         }
      """.trimIndent())
      val instance = vyne.parseJson("Book", """
         {
            "author" : {
               "firstName" : "Jimmy",
               "lastName" : "Schmitts"
            },
            "reviewers" : [ { "firstName" : "Jack" , "lastName" : "Spratt" } ],
            "title" : "Impressing people by drinking water",
            "subtitle" : null,
            "releaseDate" : "2020-11-13",
            "released" : true,
            "imdbScore" : 3.8
         }
      """.trimIndent())
      val typeNamedInstance = instance.toSerializable()
      val bytes = Cbor.encodeToByteArray(typeNamedInstance)
      val decoded = Cbor.decodeFromByteArray<SerializableTypedInstance>(bytes)
      val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)

      Benchmark.benchmark("Serializing to/from SerializableTypedInstance", warmup = 20, iterations = 50) {
         val typeNamedInstance = instance.toSerializable()
         val bytes = Cbor.encodeToByteArray(typeNamedInstance)
         val decoded = Cbor.decodeFromByteArray<SerializableTypedInstance>(bytes)
         val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)
         decodedTypedInstance
      }

       assertEquals(instance, decodedTypedInstance)

   }
}
