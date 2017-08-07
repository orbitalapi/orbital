package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.schemas.AttributeConstantValueConstraint
import io.osmosis.polymer.schemas.AttributeValueFromParameterConstraint
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

class PolymerSchemaTest {
   val taxiDef = """
         namespace polymer.example
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
         """
   val polymer = Polymer(QueryEngineFactory.noQueryEngine()).addSchema(TaxiSchema.from(taxiDef))

   @Test
   fun shouldFindLinkBetweenTypeAndProperty() {
      val path = polymer.findPath(start = "polymer.example.Client", target = "polymer.example.ClientId")
      expect(path.exists).to.equal(true)
      expect(path.description).to.equal("polymer.example.Client -[Has attribute]-> polymer.example.Client/clientId, polymer.example.Client/clientId -[Is type of]-> polymer.example.ClientId")
   }

   @Test
   fun WHEN_pathsShouldNotExist_theyReallyDont() {
      val path = polymer.findPath(start = "polymer.example.Client", target = "polymer.example.Money")
      expect(path.exists).to.equal(false)
   }

   @Test
   fun WHEN_pathDoesntExistBetweenTwoNodes_THEN_pathExistsReturnsFalse() {
      val path = polymer.findPath(start = "polymer.example.Client", target = "polymer.example.Website")
      expect(path.exists).to.equal(false)
   }

   @Test
   fun shouldParseServiceIntoSchema() {
      val service = polymer.getService("polymer.example.ClientService")
      expect(service).not.`null`
      expect(service.operations).size(3)
      expect(service.operation("getClient").parameters).size(1)
      expect(service.operation("getClient").returnType.name.fullyQualifiedName).to.equal("polymer.example.Client")
      expect(service.operation("convertMoney").parameters).size(2)
   }

   @Test
   fun shouldParseServiceContsraints() {
      val service = polymer.getService("polymer.example.ClientService")
      val operation = service.operation("convertMoney")
      expect(operation.parameters[0].constraints).size(1)
      val constraint = operation.parameters[0].constraints.first() as AttributeConstantValueConstraint
      expect(constraint.fieldName).to.equal("currency")
      expect(constraint.expectedValue.value).to.equal("GBP")
      expect(operation.contract).not.`null`
      expect(operation.contract.constraints).size(1)
      expect(operation.contract.constraints.first()).to.equal(AttributeValueFromParameterConstraint("currency", "target"))
   }

   @Test
   fun WHEN_pathExistsUsingOperation_that_itIsFound() {
      val path = polymer.findPath(start = "polymer.example.TaxFileNumber", target = "polymer.example.ClientName")
      expect(path.exists).to.equal(true)
      expect(path.description).to.equal(
         "polymer.example.TaxFileNumber -[Is parameter on]-> polymer.example.ClientService@@findClient, " +
            "polymer.example.ClientService@@findClient -[provides]-> polymer.example.Client, " +
            "polymer.example.Client -[Has attribute]-> polymer.example.Client/name, " +
            "polymer.example.Client/name -[Is type of]-> polymer.example.ClientName")
   }

}

