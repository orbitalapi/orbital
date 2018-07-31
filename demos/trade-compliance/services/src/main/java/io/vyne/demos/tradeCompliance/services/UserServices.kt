package io.vyne.demos.tradeCompliance.services

import io.vyne.tradeCompliance.*
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Service
class ClientService {
   val clients: Map<ClientId, Client> = listOf(
      Client(id = "kevin", name = "Kevin Smith", jurisdiction = "GBP"),
      Client(id = "steve", name = "Steve Jones", jurisdiction = "EUR")
   ).associateBy { it.id }

   @GetMapping("/clients/{clientId}")
   @Operation
   fun getClient(@PathVariable("clientId") clientId: ClientId): Client {
      return clients[clientId] ?: error("No client mapped for id $clientId")
   }
}

@RestController
@Service
class TraderService {
   val traders: Map<Username, User> = listOf(
      User(username = "jimmy", jurisdiction = "GBP"),
      User(username = "marty", jurisdiction = "EUR")
   ).associateBy { it.username }

   @GetMapping("/traders/{username}")
   @Operation
   fun getTrader(@PathVariable("username") username: Username): User {
      return traders[username] ?: error("No trader mapped for id $username")
   }
}
