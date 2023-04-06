package io.vyne.connectors.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import io.kotest.core.spec.style.DescribeSpec
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.io.path.toPath

// Kill or build this test.
class RawConnectionsConnectorConfigTest : DescribeSpec({
   it("should read and serialize a full hocon file") {
      val path = Resources.getResource("mixed-connections.conf")
         .toURI().toPath()

      val map = RawConnectionsConnectorConfig(path).loadAsMap()
      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(map)
      JSONAssert.assertEquals(
         json, """{
    "jdbc" : {
        "another-connection" : {
            "connectionName" : "another-connection",
            "connectionParameters" : {
                "database" : "transactions",
                "host" : "our-db-server",
                "password" : "super-secret",
                "port" : "2003",
                "username" : "jack"
            },
            "jdbcDriver" : "POSTGRES"
        }
    },
    "kafka" : {
        "some-connection" : {
            "something" : "foo"
        }
    }
}
""", true
      )
   }

})
