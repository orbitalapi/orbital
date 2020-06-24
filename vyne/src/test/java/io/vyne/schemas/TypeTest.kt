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

      type Person {
         name : FirstName
      }
      type alias Human as Person

   """.trimIndent()
   val schema = TaxiSchema.from(taxi)

   @Test
   fun isPrimitiveDeriviedCorrectly() {
      schema.type("lang.taxi.String").isPrimitive.should.be.`true`
      // Alias
      schema.type("Name").isPrimitive.should.be.`false`
      // Inherited alias
      schema.type("FirstName").isPrimitive.should.be.`false`

      // Array
      schema.type("NameList").isPrimitive.should.be.`false`

      // Hmm... not sure bout this, I guess technicaly it is primitive, since it's
      // Array<FirstName>, but that feels wrong
      schema.type("FirstName[]").isPrimitive.should.be.`true`
   }

   @Test
   fun isScalarDerivesCorrectly() {
      schema.type("lang.taxi.String").isScalar.should.be.`true`
      // Alias
      schema.type("Name").isScalar.should.be.`true`
      // Inherited alias
      schema.type("FirstName").isScalar.should.be.`true`

      // Array
      schema.type("NameList").isScalar.should.be.`false`
      schema.type("FirstName[]").isScalar.should.be.`false`

      schema.type("Person").isScalar.should.be.`false`
      // Type alias to object
      schema.type("Human").isScalar.should.be.`false`
   }

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

   @Test
   fun formattedTypesShouldResolveAliasesToTheirUnformattedType() {
      val schema = TaxiSchema.from("""
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model ThingWithInlineInstant {
            eventDate : Instant( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSX" )
         }
      """)
      schema.type("EventDate").resolveAliases().fullyQualifiedName.should.equal("EventDate")
      schema.type(schema.type("Source").attribute("eventDate").type).resolveAliases().fullyQualifiedName.should.equal("EventDate")
      schema.type(schema.type("ThingWithInlineInstant").attribute("eventDate").type).resolveAliases().fullyQualifiedName.should.equal("lang.taxi.Instant")
   }

   @Test
   fun whenTwoUnformattedTypesAreAssignableThenTheyAreAssignableWhenFormatted() {
      val schema = TaxiSchema.from("""
         type EventDate inherits Instant
         type LunchTime inherits Instant

         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSX" )
         }
      """)
      val sourceType = schema.type(schema.type("Source").attribute("eventDate").type)
      val targetType = schema.type(schema.type("Target").attribute("eventDate").type)
      sourceType.isAssignableTo(targetType).should.be.`true`
      targetType.isAssignableTo(sourceType).should.be.`true`

      schema.type("LunchTime").isAssignableTo(sourceType).should.be.`false`
      schema.type("LunchTime").isAssignableTo(targetType).should.be.`false`
      targetType.isAssignableTo(schema.type("LunchTime")).should.be.`false`
      sourceType.isAssignableTo(schema.type("LunchTime")).should.be.`false`
   }

   @Test
   fun typeAliasesOnFormattedTypesShouldResolveCorrectly() {
      val schema = TaxiSchema.from("""
         type alias EventDate as Instant
      """)
      schema.type("EventDate").resolveAliases().fullyQualifiedName.should.equal("EventDate")
   }
}
