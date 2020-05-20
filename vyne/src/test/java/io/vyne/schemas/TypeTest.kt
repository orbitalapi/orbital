package io.vyne.schemas

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Assert.*
import org.junit.Test

class TypeTest {
   // Note: This clearly shows why type alias for primitives was a bad idea
   // type alias Name as String means that Name[] and String[] are the same.
   // And theres no real way of addressing that
   // We need a construct like type Name : String support
   // See LENS-72
   val taxi = """
      type alias EyeColour as String
      type alias Name as String
      type alias Identifier as Name

      type FirstName inherits Name
      type alias GivenName as FirstName

      type alias NameList as FirstName[]

   """.trimIndent()
   val schema = TaxiSchema.from(taxi)

   @Test
   fun aliasedPrimitivesCannotBeAssignedToEachOther() {
      schema.type("Name").isAssignableTo(schema.type("EyeColour")).should.be.`false`
      schema.type("EyeColour").isAssignableTo(schema.type("Name")).should.be.`false`
   }

   @Test
   fun isAssignableToShouldBeRight() { // Fuck it, you come up with a better name.
      schema.type("FirstName").isAssignableTo(schema.type("Name")).should.be.`true`
      schema.type("GivenName").isAssignableTo(schema.type("Name")).should.be.`true`
      schema.type("Name").isAssignableTo(schema.type("FirstName")).should.be.`false`
      schema.type("Name").isAssignableTo(schema.type("GivenName")).should.be.`false`
   }

   @Test
   fun isAssignableConsidersVarianceRules() {
      schema.type("FirstName[]").isAssignableTo(schema.type("Name[]")).should.be.`true`
      schema.type("GivenName[]").isAssignableTo(schema.type("Name[]")).should.be.`true`
      schema.type("Name[]").isAssignableTo(schema.type("FirstName[]")).should.be.`false`
      schema.type("Name[]").isAssignableTo(schema.type("GivenName[]")).should.be.`false`
   }

   @Test
   fun isAssignableAcrossTypeAliasesConsiderVarianceRules() {
      schema.type("NameList").isAssignableTo(schema.type("FirstName[]")).should.be.`true`
      schema.type("FirstName[]").isAssignableTo(schema.type("NameList")).should.be.`true`
      schema.type("GivenName[]").isAssignableTo(schema.type("NameList")).should.be.`true`
      schema.type("NameList").isAssignableTo(schema.type("GivenName[]")).should.be.`true`
   }

   @Test
   fun inheritsFromConsidersAliases() {
      schema.type("FirstName").inheritsFrom(schema.type("Name")).should.be.`true`
      schema.type("GivenName").inheritsFrom(schema.type("Name")).should.be.`true`


      schema.type("Name").inheritsFrom(schema.type("FirstName")).should.be.`false`
      schema.type("Name").inheritsFrom(schema.type("GivenName")).should.be.`false`
   }

   @Test
   fun isCollectionParsesCorrectly() {
      schema.type("Name[]").isCollection.should.be.`true`
      schema.type("NameList").isCollection.should.be.`true`
      schema.type("FirstName[]").isCollection.should.be.`true`
      schema.type("GivenName[]").isCollection.should.be.`true`
      schema.type("GivenName").isCollection.should.be.`false`
      schema.type("Name").isCollection.should.be.`false`
   }
}
