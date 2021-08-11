package io.vyne.queryService.query

import com.winterbe.expekt.should
import io.vyne.models.SerializableTypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.toSerializable
import io.vyne.testVyne
import io.vyne.utils.Benchmark
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SerializableTypedInstanceTest {

   @Test
   fun `can convert downcasted dateTime values to and from bytes`(): Unit = runBlocking {
      // We encountered an issue where after a value has been projected into a type that
      // downcasts it through a format (ie., applying a format that truncates the time data from a date time)
      // then it cannot be deserialized, as the data is lost.
      // To address this, we are doing SerDe on the unformatted value.
      // This test ensures that the data surivies the journey, and that the format can still be applied
      // later.
      val (vyne, _) = testVyne(
         """
         type TransactionDateTime inherits Instant
         model InputType {
            transactionDate : TransactionDateTime
         }
         model TargetType {
            transactionDate : TransactionDateTime(@format = "dd-MMM-yy")
         }
      """
      )
      val parsed = vyne.parseJson("InputType", """{ "transactionDate" : "2020-10-03T23:15:00" } """)
      val output = vyne.from(parsed).build("TargetType")
         .results.toList()
      val reformattedDate = output.first()
         .toSerializable()

      val bytes = reformattedDate.toBytes()
      val decoded = SerializableTypedInstance.fromBytes(bytes)
      val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)

      val rawValue = decodedTypedInstance.toRawObject()
      rawValue.should.equal(mapOf("transactionDate" to "03-Oct-20"))
   }

   @Test
   fun `can convert type named instance to and from bytes`() {
      val (vyne, _) = testVyne(
         """
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
      """.trimIndent()
      )
      val instance = vyne.parseJson(
         "Book", """
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
      """.trimIndent()
      )
      val typeNamedInstance = instance.toSerializable()
      val bytes = typeNamedInstance.toBytes()
      val decoded = SerializableTypedInstance.fromBytes(bytes)
      val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)

      Benchmark.benchmark("Serializing to/from SerializableTypedInstance", warmup = 20, iterations = 50) {
         val typeNamedInstance = instance.toSerializable()
         val bytes = typeNamedInstance.toBytes()
         val decoded = SerializableTypedInstance.fromBytes(bytes)
         val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)
         decodedTypedInstance
      }

      assertEquals(instance, decodedTypedInstance)

   }
}
