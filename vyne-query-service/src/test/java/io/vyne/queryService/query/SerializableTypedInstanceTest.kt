package io.vyne.queryService.query

import com.winterbe.expekt.should
import io.vyne.models.SerializableTypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJson
import io.vyne.models.toSerializable
import io.vyne.schemas.fqn
import io.vyne.utils.Benchmark
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.Test

class SerializableTypedInstanceTest {
   val vyne = io.vyne.testVyne(
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
   ).first
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
   ) as TypedObject

   @Test
   fun `values are the same after deserialization`() {
      val (original, deser) = encodeAndDecodeTypedInstance()
      deser.toRawObject().should.equal(original.toRawObject())
   }
   @Test
   fun `null values are returned as typed nulls as deserialization`() {
      val (original, deser) = encodeAndDecodeTypedInstance()
      val nullValue = deser["subtitle"] as TypedNull
      nullValue.typeName.should.equal("SubTitle")
   }

   @Test
   fun `can convert array`() {
      val (original, deser) = encodeAndDecodeTypedInstance()
      deser["reviewers"].typeName.should.equal("Person[]".fqn().parameterizedName)
   }

   @Test
   fun `type names match`() {
      val (original, deser) = encodeAndDecodeTypedInstance()
      deser.typeName.should.equal(original.typeName)

      fun compareTypes(expected:TypedObject, actual:TypedObject) {
         actual.type.attributes.forEach { (fieldName, field) ->
            expected[fieldName].typeName.should.equal(actual[fieldName].typeName)

            if (actual[fieldName] is TypedObject) {
               compareTypes(expected[fieldName] as TypedObject, actual[fieldName] as TypedObject)
            }
         }
      }

      compareTypes(original,deser)
   }



   @Test
   fun benchmark() {
      Benchmark.benchmark("Serializing to/from SerializableTypedInstance", warmup = 20, iterations = 5000) {
         val typeNamedInstance = instance.toSerializable()
         val bytes = Cbor.encodeToByteArray(typeNamedInstance)
         val decoded = Cbor.decodeFromByteArray<SerializableTypedInstance>(bytes)
         val decodedTypedInstance = decoded.toTypedInstance(vyne.schema)
         decodedTypedInstance
      }
   }

   /**
    * Returns the original typed instance, and the deserialzied version
    */
   private fun encodeAndDecodeTypedInstance(original: TypedObject = instance): Pair<TypedObject, TypedObject> {
      val typeNamedInstance = original.toSerializable()
      val bytes = Cbor.encodeToByteArray(typeNamedInstance)
      val decoded = Cbor.decodeFromByteArray<SerializableTypedInstance>(bytes)
      val decodedTypedInstance = decoded.toTypedInstance(vyne.schema) as TypedObject
      return original to decodedTypedInstance
   }

   @Test
   fun `null values are deserialized correctly`() {

   }
}
