package io.vyne

import com.winterbe.expekt.expect
import io.vyne.query.QueryEngineFactory
import io.vyne.schemas.AttributeConstantValueConstraint
import io.vyne.schemas.AttributeValueFromParameterConstraint
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.AttributePath
import org.junit.Test

class VyneSchemaTest {
   val taxiDef = """
         namespace vyne.example
         type Invoice {
            clientId : ClientId
            amount : Money
         }
         type Money {
            value : MoneyAmount as Decimal
            currency : CurrencySymbol as String
         }
         type Client {
            @Id
            clientId : ClientId as String
            name : ClientName as String
         }
         // Entirely unrelated type
         type Website {}

         type alias TaxFileNumber as String

         service ClientService {
            operation findClient(TaxFileNumber):Client
            operation getClient(ClientId):Client

            operation convertMoney(Money(currency = 'GBP'),target : CurrencySymbol):Money( currency = target )
         }

         parameter type SomeRequestType {
            clientId : ClientId as String
         }

         type TypeWithStringField {
            name : String
         }
         """
   val vyne = Vyne(QueryEngineFactory.default()).addSchema(TaxiSchema.from(taxiDef))

//   @Test
//   fun shouldFindLinkBetweenTypeAndProperty() {
//      val path = vyne.query().findPath(start = "vyne.example.Client", target = "vyne.example.ClientId")
//      expect(path.exists).to.equal(true)
//      expect(path.description).to.equal("vyne.example.Client -[Has attribute]-> vyne.example.Client/clientId, vyne.example.Client/clientId -[Is type of]-> vyne.example.ClientId")
//   }
//
//   @Test
//   fun WHEN_pathsShouldNotExist_theyReallyDont() {
//      val path = vyne.query().findPath(start = "vyne.example.Client", target = "vyne.example.Money")
//      expect(path.exists).to.equal(false)
//   }
//
//   @Test
//   fun WHEN_pathDoesntExistBetweenTwoNodes_THEN_pathExistsReturnsFalse() {
//      val path = vyne.query().findPath(start = "vyne.example.Client", target = "vyne.example.Website")
//      expect(path.exists).to.equal(false)
//   }

   @Test
   fun shouldParseServiceIntoSchema() {
      val service = vyne.getService("vyne.example.ClientService")
      expect(service).not.`null`
      expect(service.operations).size(3)
      expect(service.operation("getClient").parameters).size(1)
      expect(service.operation("getClient").returnType.name.fullyQualifiedName).to.equal("vyne.example.Client")
      expect(service.operation("convertMoney").parameters).size(2)
   }

   @Test
   fun primitiveTypesShouldBeAdded() {
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
   fun shouldParseServiceContsraints() {
      val service = vyne.getService("vyne.example.ClientService")
      val operation = service.operation("convertMoney")
      expect(operation.parameters[0].constraints).size(1)
      val constraint = operation.parameters[0].constraints.first() as AttributeConstantValueConstraint
      expect(constraint.fieldName).to.equal("currency")
      expect(constraint.expectedValue.value).to.equal("GBP")
      expect(operation.contract).not.`null`
      expect(operation.contract.constraints).size(1)
      expect(operation.contract.constraints.first()).to.equal(AttributeValueFromParameterConstraint("currency", AttributePath.from("target")))
   }

   @Test
   fun shouldDetectParamObjects() {
      val type = vyne.getType("vyne.example.SomeRequestType")
      expect(type.isParameterType).to.be.`true`
   }

   @Test
   fun shouldParseTypeAliases() {
      val type = vyne.getType("vyne.example.TaxFileNumber")
      expect(type.aliasForType!!.name).to.equal("String")
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

