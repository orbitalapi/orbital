package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

class PolymerSchemaTest {
   val taxiDef = """
            namespace polymer.example
            type Invoice {
               clientId : ClientId
            }
            type Client {
               @Id
               clientId : ClientId as String
               name : ClientName as String
            }
         """
   val polymer = Polymer().addSchema(TaxiSchema.from(taxiDef))

   @Test
   fun shouldFindAPropertyOnAType() {
      val path = polymer.findPath(start = "polymer.example.Client", target = "polymer.example.ClientId")
      expect(path.exists).to.equal(true)
      expect(path.description).to.equal("Client -HasPropertyOf-> ClientId")
   }
}

