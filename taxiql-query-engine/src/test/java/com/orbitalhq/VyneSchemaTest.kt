package com.orbitalhq

import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.schemas.EnumValue
import com.orbitalhq.schemas.Modifier
import com.orbitalhq.schemas.ScalarEnumValueSupplier
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import org.junit.Test

class VyneSchemaTest {
   private fun vyneWithTestSchema():Vyne {
      val taxiDef = """
         namespace vyne.example
         type ClientId inherits String
         type Invoice {
            clientId : ClientId
            amount : Money
         }
         type Money {
            value : MoneyAmount inherits Decimal
            currency : CurrencySymbol inherits String
         }
         type Client {
            @Id
            clientId : ClientId
            name : ClientName inherits String
            clientType : ClientType
            emailAddresses : EmailAddress[]
         }

         type EmailAddress inherits String

         enum ClientType {
            INDIVIDUAL,
            COMPANY
         }

         enum BankDirection {
            BankBuys,
            BankSell
         }

         enum BankXDirection {
         [[Dummy TypeDoc]]
            BUY(0) synonym of BankDirection.BankBuys,
            SELL(1) synonym of BankDirection.BankSell
         }



         // Entirely unrelated type
         type Website {}

         type TaxFileNumber inherits String

         service ClientService {
            operation findClient(TaxFileNumber):Client
            operation getClient(ClientId):Client

            operation convertMoney(Money(this.currency == 'GBP'),target : CurrencySymbol):Money( this.currency == target )
         }

         parameter type SomeRequestType {
            clientId : ClientId as String
         }

         type TypeWithStringField {
            name : String
         }
         """
      return Vyne(QueryEngineFactory.default()).addSchema(TaxiSchema.from(taxiDef))
   }


   @Test
   fun shouldParseServiceIntoSchema() {
      val vyne = vyneWithTestSchema()
      val service = vyne.getService("vyne.example.ClientService")
      expect(service).not.`null`
      expect(service.operations).size(3)
      expect(service.operation("getClient").parameters).size(1)
      expect(service.operation("getClient").returnType.name.fullyQualifiedName).to.equal("vyne.example.Client")
      expect(service.operation("convertMoney").parameters).size(2)
   }

   @Test
   fun primitiveTypesShouldBeAdded() {
      val vyne = vyneWithTestSchema()
      expect(vyne.type("lang.taxi.String")).to.be.not.`null`
   }

   @Test // TODO : Added this test as a reminder to come back. This doesn't work at the moment, and it should ... FIXME.
   fun given_primitiveTypeIsDeclaredAsAnAlias_then_thePrimitiveTypeIsPresentWithinTheParsedSchema() {
      val taxiDef = """
          parameter type SomeRequestType {
            clientId : ClientId as String
         }
"""
      val schema = TaxiSchema.from(taxiDef)
      expect(schema.hasType("lang.taxi.String")).to.be.`true`
   }

   @Test
   fun shouldBeAbleToLookUpViaShortName() {
      val vyne = vyneWithTestSchema()
      val invoiceType = vyne.getType("Invoice")
      expect(invoiceType.fullyQualifiedName).to.equal("vyne.example.Invoice")
   }

   @Test
   fun canLookUpParameterisedType() {
      val taxiDef = """
          type EmailAddress inherits String
      """.trimIndent()
      val schema = TaxiSchema.from(taxiDef)
      expect(schema.hasType("lang.taxi.Array"))
      expect(schema.hasType("EmailAddress"))
      expect(schema.hasType("lang.taxi.Array<EmailAddress>"))

      expect(schema.type("lang.taxi.Array")).to.be.not.`null`
      expect(schema.type("EmailAddress")).to.be.not.`null`
      expect(schema.type("lang.taxi.Array<EmailAddress>")).to.be.not.`null`
   }

   @Test
   fun cannotLookupByShortNameWhenItIsAmiguous() {
      val taxiDef = """
          namespace foo {
            type Customer {
               firstName : FirstName inherits String
            }
          }
          namespace bar {
            type Customer {
               lastName : LastName inherits String
            }
          }
      """.trimIndent()
      val schema = TaxiSchema.from(taxiDef)
      expect(schema.hasType("Customer")).to.be.`false`
      expect(schema.hasType("foo.Customer")).to.be.`true`
      expect(schema.hasType("bar.Customer")).to.be.`true`
      expect(schema.hasType("FirstName")).to.be.`true`
      expect(schema.hasType("foo.FirstName")).to.be.`true`
      expect(schema.hasType("LastName")).to.be.`true`
      expect(schema.hasType("bar.LastName")).to.be.`true`
   }

   @Test
   fun shouldDetectParamObjects() {
      val vyne = vyneWithTestSchema()
      val type = vyne.getType("vyne.example.SomeRequestType")
      expect(type.isParameterType).to.be.`true`
   }

   @Test
   fun shouldParseEnumTypes() {
      val vyne = vyneWithTestSchema()
      val type = vyne.getType("vyne.example.BankXDirection")
      expect(type.modifiers).to.contain(Modifier.ENUM)

      expect(type.enumValues).to.have.size(2)
      expect(type.enumValues).to.contain(EnumValue("BUY", 0, listOf("vyne.example.BankDirection.BankBuys"), "Dummy TypeDoc", ScalarEnumValueSupplier))
   }


   @Test
   fun arraysShouldBeParsedToCollectionTypes() {
      val taxiDef = """
service Test {
   operation `find`():EmailAddress[]
}
type EmailAddress inherits String
      """.trimIndent()
      val schema = TaxiSchema.from(taxiDef)
      val operation = schema.service("Test").operation("`find`")
      val returnType = operation.returnType
      val emailAddressType = schema.type("EmailAddress")
      expect(returnType.name.name).to.equal("Array")
      expect(returnType.typeParameters).to.have.size(1)
      expect(returnType.typeParameters.first()).to.equal(emailAddressType)
   }

   @Test
   fun `parses return types of stream correctly`() {
      val schema = TaxiSchema.from("""
         model Person {}
         service PersonService {
            operation streamPeople():Stream<Person>
         }
      """.trimIndent())
      val operation = schema.service("PersonService").operation("streamPeople")
      operation.returnType.name.parameterizedName.should.equal("lang.taxi.Stream<Person>")
      operation.returnType.typeParameters.should.have.size(1)
   }


//   @Test
//   fun WHEN_pathExistsUsingOperation_that_itIsFound() {
//      val path = vyne.query().findPath(start = "vyne.example.TaxFileNumber", target = "vyne.example.ClientName")
//      expect(path.exists).to.equal(true)
//      expect(path.description).to.equal(
//         "vyne.example.TaxFileNumber -[Is parameter on]-> vyne.example.ClientService@@findClient, " +
//            "vyne.example.ClientService@@findClient -[provides]-> vyne.example.Client, " +
//            "vyne.example.Client -[Has attribute]-> vyne.example.Client/name, " +
//            "vyne.example.Client/name -[Is type of]-> vyne.example.ClientName")
//   }

}

