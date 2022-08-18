package io.vyne.spring

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.schema.publisher.loaders.FileSchemaSourceProvider
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import io.vyne.schemas.OperationNames
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Ignore
import org.junit.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Paths

class SchemaProvidersTest {
   val source = """
type Person {
   firstName : FirstName as String
   lastName : LastName as String
   address : Address
}
type Book {
   author : Person
}
type Address {
   streetNumber : StreetNumber as Int
   streetName : StreetName as String
}

service MyService {
   operation findPerson(FirstName):Person
   operation deletePerson(FirstName):Boolean
}
   """.trimIndent()

   @Test
   fun shouldBeAbleToGetSubsetOfSchema() {
      val provider = SimpleTaxiSchemaProvider(source);

      val schema = provider.schema(listOf("Person"), includePrimitives = false)
      expect(schema.hasType("Person")).to.be.`true`
      expect(schema.hasType("Address")).to.be.`true`
      expect(schema.hasType("Book")).to.be.`false`

      // Should contain aliased primitives, but not primitives themselves
      expect(schema.hasType("FirstName")).to.be.`true`
      expect(schema.hasType("lang.taxi.String")).to.be.`false`
   }

   @Test
   fun whenPrimtiviesAreREquested_then_theyAreIncludedInSubsetSchema() {
      val provider = SimpleTaxiSchemaProvider(source);

      val schema = provider.schema(listOf("Person"), includePrimitives = true)
      expect(schema.hasType("lang.taxi.String")).to.be.`true`
   }

   @Test
   fun canFilterSchemaByOperation() {
      val provider = SimpleTaxiSchemaProvider(source)

      val operationName = OperationNames.name("MyService", "findPerson")
      val schema = provider.schema(listOf(operationName))

      expect(schema.hasType("FirstName")).to.be.`true`
      expect(schema.hasType("Person")).to.be.`true`
      expect(schema.hasType("LastName")).to.be.`true`
      expect(schema.hasType("Address")).to.be.`true`

      expect(schema.hasOperation(operationName.fqn())).to.be.`true`
      expect(schema.hasOperation(OperationNames.qualifiedName("MyService", "deletePerson"))).to.be.`false`
   }

   @Test
   fun canFilterSchemaByService() {
      val provider = SimpleTaxiSchemaProvider(source)

      val schema = provider.schema(listOf("MyService"))

      expect(schema.hasOperation(OperationNames.qualifiedName("MyService", "findPerson"))).to.be.`true`
      expect(schema.hasOperation(OperationNames.qualifiedName("MyService", "deletePerson"))).to.be.`true`
   }

   @Test
   @Ignore("I don't think this is supported any more")
   fun `should be able to fetch schema from a file in classpath`() {
      val absolutePath = PathMatchingResourcePatternResolver().getResource("foo.taxi").file.absolutePath
      val provider = FileSchemaSourceProvider(Paths.get(absolutePath))
      expect(provider.versionedSources).size.equal(1)
      val schema = TaxiSchema.from(provider.versionedSources)
      schema.hasType("vyne.example.Client").should.be.`true`
   }
}
