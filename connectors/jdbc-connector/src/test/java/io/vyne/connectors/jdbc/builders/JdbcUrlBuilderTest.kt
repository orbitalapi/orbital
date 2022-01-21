package io.vyne.connectors.jdbc.builders

import com.winterbe.expekt.should
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.MissingConnectionParametersException
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.jdbc.JdbcUrlBuilder
import org.junit.Test
import kotlin.test.assertFailsWith

class JdbcUrlBuilderTest {

   @Test
   fun `throws exception is required fields are not populated`() {
      val exception = assertFailsWith<MissingConnectionParametersException> {
         val params = listOf(
            ConnectionDriverParam("host", SimpleDataType.STRING),
            ConnectionDriverParam("port", SimpleDataType.NUMBER),
         )
         val inputs = mapOf("host" to "localhost") // port is missing
         JdbcUrlBuilder.assertAllParametersPresent(params, inputs)
      }
      exception.message.should.equal("The following parameters were not provided: port")
   }

   @Test
   fun `does not throw if optional param is missing`() {
      val params = listOf(
         ConnectionDriverParam("host", SimpleDataType.STRING),
         ConnectionDriverParam("port", SimpleDataType.NUMBER, required = false),
      )
      val inputs = mapOf("host" to "localhost") // port is missing
      JdbcUrlBuilder.assertAllParametersPresent(params, inputs)
   }

   @Test
   fun `returns map populated with defaults if not present in input`() {
      val params = listOf(
         ConnectionDriverParam("host", SimpleDataType.STRING),
         ConnectionDriverParam("port", SimpleDataType.STRING, defaultValue = 5542),
         ConnectionDriverParam("database", SimpleDataType.STRING, defaultValue = "foo"),
      )
      val inputs = mapOf(
         "host" to "localhost",
         // port is missing, use the default
         "database" to "testDb" // testDb contains a default, but don't use it
      )
      val expandedParams = JdbcUrlBuilder.assertAllParametersPresent(params, inputs)
      expandedParams.should.equal(
         mapOf(
            "host" to "localhost",
            "port" to 5542,
            "database" to "testDb"
         )
      )
   }
}
