package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

class VyneProjectionBatchingTests {

   val schema = TaxiSchema.from(
      """
         model Client {
            @Id
            clientId : ClientId as Int
            clientName : ClientName as String
         }
         model Order {
            orderId : OrderId as Int
            clientId : ClientId
         }
         model OutputModel {
            orderId : OrderId
            @FirstNotEmpty
            clientName : ClientName
         }
         service ClientService {
            @StubResponse("findSingleClient")
            operation findSingleClient(ClientId):Client
            @StubResponse("findClients")
            operation findClients(ClientId[]):Client[]
         }
      """
   )

   // Set up a few orders
   val ordersJson = """[
         |{ "orderId" : 1 , "clientId" : 1 },
         |{ "orderId" : 2 , "clientId" : 2 },
         |{ "orderId" : 21 , "clientId" : 2 },
         |{ "orderId" : 3 , "clientId" : 3 }]
      """.trimMargin()
   val orders = TypedInstance.from(schema.type("Order[]"), ordersJson, schema, source = Provided) as TypedCollection

   fun buildClient(clientId: Int, clientName: String? = "ClientName-$clientId"): TypedInstance {
      val client = mapOf("clientId" to clientId, "clientName" to clientName)
      return TypedInstance.from(schema.type("Client"), client, schema, source = Provided)
   }

   @Test
   fun `when projecting multiple records http operations are pre-batched`() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("findSingleClient") { _, parameters: List<Pair<Parameter, TypedInstance>> ->
         val clientId = parameters[0].second.value as Int
         listOf(buildClient(clientId))
      }
      stub.addResponse("findClients") { _, params ->
         val clientIds = params[0].second.value as List<TypedValue>
         val clients = clientIds.map { buildClient(it.value as Int) }
         TypedCollection.from(clients)
      }
      val results = runBlocking {vyne.from(orders).build("OutputModel[]").results.toList()}

      stub.invocations["findClients"].should.have.size(1)
      // there should be no calls to the findSingle endpoint, as they were all resolved
      stub.invocations["findSingleClient"].should.be.`null`

      // let's be sure the right values got matched
      val outputModels = results.get(0) as TypedCollection
      outputModels.orderWithId(1)["clientName"].value.should.equal("ClientName-1")
      outputModels.orderWithId(2)["clientName"].value.should.equal("ClientName-2")
      outputModels.orderWithId(21)["clientName"].value.should.equal("ClientName-2")
      outputModels.orderWithId(3)["clientName"].value.should.equal("ClientName-3")
   }

   @Test
   fun `if batched operation omits value in result then other operations are called`() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("findSingleClient") { _, parameters: List<Pair<Parameter, TypedInstance>> ->
         val clientId = parameters[0].second.value as Int
         listOf(buildClient(clientId))
      }
      stub.addResponse("findClients") { _, params ->
         val clientIds = params[0].second.value as List<TypedValue>
         val clients = clientIds
            .filter { it.value as Int != 2 } // Don't provide value 2
            .map { buildClient(it.value as Int) }
         TypedCollection.from(clients)
      }
      val results = runBlocking {vyne.from(orders).build("OutputModel[]").results.toList()}
      val output = results.get(0) as TypedCollection

      val orderId2 =  output.orderWithId(2)
      // The value should still have been populated
      orderId2["clientName"].value.should.equal("ClientName-2")

      // We should see a single call to the findSingleClient operation
      stub.invocations["findSingleClient"]!!.should.have.size(1)
   }

   @Test
//   @Ignore // Not yet implemented.
   fun `if batched operation returns null value in result then other operations are called`() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("findSingleClient") { _, parameters: List<Pair<Parameter, TypedInstance>> ->
         val clientId = parameters[0].second.value as Int
         listOf(buildClient(clientId))
      }
      stub.addResponse("findClients") { _, params ->
         val clientIds = params[0].second.value as List<TypedValue>
         val clients = clientIds
            .map {
               val clientId = it.value as Int
               if (clientId == 2) {
                  buildClient(clientId, clientName = null)
               } else {
                  buildClient(clientId)
               }
            }
         TypedCollection.from(clients)
      }
      val results = runBlocking {vyne.from(orders).build("OutputModel[]").results.toList()}
      val output = results.get(0) as TypedCollection
      val orderId2 = output.first { (it as TypedObject)["orderId"].value == 2 } as TypedObject
      // The value should still have been populated
      orderId2["clientName"].value.should.equal("ClientName-2")

      // We should see a single call to the findSingleClient operation
      stub.invocations["findSingleClient"]!!.should.have.size(1)
   }
}

private fun TypedCollection.orderWithId(id: Int) :TypedObject {
 return  this.first { (it as TypedObject)["orderId"].value == id } as TypedObject
}

