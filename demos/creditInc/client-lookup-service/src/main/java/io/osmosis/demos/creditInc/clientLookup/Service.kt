package io.osmosis.demos.creditInc.clientLookup

import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


// Note : This is a redeclaration of the client-type
// used elsewhere in this demo
// This is intentional
// It allows microservices to declare their own version of
// the entity, with the attributes appropriate to it's service
// and reduces coupling between the services
@DataType("polymer.creditInc.Client")
data class Client(@field:DataType("polymer.creditInc.ClientId") val clientId: String,
                  @field:DataType("polymer.creditInc.ClientName") val clientName: String,
                  @field:DataType("isic.uk.SIC2008") val sicCode: String
)


@RestController
@Service
class ClientLookupService {
   val clients = listOf(
      Client("jim01", "Jim's Bar & Grill", "2008-123456")
   )
   val clientsById = clients.associateBy { it.clientId }

   @RequestMapping("/clients/{id}")
   @Operation
   fun findClientById(@DataType("polymer.creditInc.ClientId") @PathVariable("id") clientId: String): Client {
      return clientsById[clientId] ?: error("No client with $clientId was found")
   }
}
