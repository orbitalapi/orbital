package com.orbitalhq.pipelines.runner.transport.http

import com.winterbe.expekt.should
import com.orbitalhq.pipelines.jet.api.transport.http.ParameterMapToTypeResolver
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.Test

class ParameterMapToTypeResolverTest {

   private val schema = TaxiSchema.from(
      """
      model Person {
         firstName : FirstName as String
         lastName : LastName as String
      }
      service PersonService {
         operation findPerson(givenName : FirstName) : Person
      }
   """.trimIndent()
   )

   @Test
   fun `if parameter exists with provided name then resolve by name`() {
      val (_, operation) = schema.operation(OperationNames.qualifiedName("PersonService", "findPerson"))
      val resolved = ParameterMapToTypeResolver.resolveToTypes(
         mapOf("givenName" to "Jimmy"),
         operation
      )
      resolved.keys.first().type.fullyQualifiedName.should.equal("FirstName")
   }

   @Test
   fun `if parameter exists with provided type then resolve by type`() {
      val (_, operation) = schema.operation(OperationNames.qualifiedName("PersonService", "findPerson"))
      val resolved = ParameterMapToTypeResolver.resolveToTypes(
         mapOf("FirstName" to "Jimmy"),
         operation
      )
      resolved.keys.first().type.fullyQualifiedName.should.equal("FirstName")
   }

   @Test
   fun `if paramaeter does not match on either name or type then it is excluded from the result`() {
      val (service, operation) = schema.operation(OperationNames.qualifiedName("PersonService", "findPerson"))
      val resolved = ParameterMapToTypeResolver.resolveToTypes(
         mapOf("Invalid" to "Jimmy"),
         operation
      )
      resolved.should.be.empty
   }
}
