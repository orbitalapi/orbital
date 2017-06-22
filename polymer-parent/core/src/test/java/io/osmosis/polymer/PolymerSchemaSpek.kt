package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class PolymerSchemaSpek : Spek({
   describe("Schema operations") {
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

      it("should find a link between two nodes") {

      }
      it("should find a property on a type") {
         val path = polymer.findPath(start = "polymer.example.Client", target = "polymer.example.ClientId")
         expect(path.exists).to.equal(true)
         expect(path.description).to.equal("Client -HasPropertyOf-> ClientId")
      }
   }
})

