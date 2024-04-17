package com.orbitalhq.connectors.config.mongodb

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class MongoConnectionConfigurationTest {

   @Test
   fun `getUiDisplayProperties should obfuscate password`() {
      val testCases = listOf(
         "mongodb+srv://orbital:D1fficultP%40ssw0rd@orbital.xxxx.mongodb.net/?retryWrites=true&w=majority&appName=Orbital",
         "mongodb://user:PASSWORD@localhost:27017/test",
         "mongodb+srv://user:PASSWORD@cluster.mongodb.net/test"
      )

      testCases.forEach { connectionString ->
         val configuration = MongoConnectionConfiguration(
            connectionName = "TestConnection",
            connectionParameters = mapOf(
               "connectionString" to connectionString
            )
         )

         val displayProperties = configuration.getUiDisplayProperties()

         assertEquals(
            configuration.obfuscatePassword(connectionString),
            displayProperties["connectionString"]
         )
      }
   }

}
