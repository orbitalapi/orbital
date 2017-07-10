package io.osmosis.polymer

import com.winterbe.expekt.expect
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

         service ClientService {
            operation getClient(ClientId):Client
            operation convertMoney(Money,CurrencySymbol):Money
         }
         """
   val polymer = Polymer().addSchema(TaxiSchema.from(taxiDef))

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
      expect(service.operations).size(2)
      expect(service.operation("getClient").parameters).size(1)
      expect(service.operation("getClient").returnType.name.fullyQualifiedName).to.equal("polymer.example.Client")
      expect(service.operation("convertMoney").parameters).size(2)
   }
}

