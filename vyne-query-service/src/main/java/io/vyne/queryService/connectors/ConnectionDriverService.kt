package io.vyne.queryService.connectors

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.kafka.KafkaConnection
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConnectionDriverService {

   @GetMapping("/api/connections/drivers")
   fun listAvailableDrivers(): List<ConnectionDriverOptions> {
      return JdbcDriver.driverOptions + KafkaConnection.driverOptions
   }

}
