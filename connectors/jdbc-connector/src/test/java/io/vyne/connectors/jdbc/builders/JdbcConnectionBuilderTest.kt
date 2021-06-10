package io.vyne.connectors.jdbc.builders

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.JdbcConnectionBuilder
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.MissingConnectionParametersException
import lang.taxi.types.PrimitiveType
import org.junit.Test
import kotlin.test.assertFailsWith

class JdbcConnectionBuilderTest {

   @Test
   fun `throws exception is required fields are not populated`() {
      val exception = assertFailsWith<MissingConnectionParametersException> {
         val params = listOf(
            JdbcConnectionParam("host", PrimitiveType.STRING),
            JdbcConnectionParam("port", PrimitiveType.INTEGER),
         )
         val inputs = mapOf("host" to "localhost") // port is missing
         JdbcConnectionBuilder.assertAllParametersPresent(params, inputs)
      }
      exception.message.should.equal("The following parameters were not provided: port")
   }

   @Test
   fun `does not throw if optional param is missing`() {
      val params = listOf(
         JdbcConnectionParam("host", PrimitiveType.STRING),
         JdbcConnectionParam("port", PrimitiveType.INTEGER, required = false),
      )
      val inputs = mapOf("host" to "localhost") // port is missing
      JdbcConnectionBuilder.assertAllParametersPresent(params, inputs)
   }

   @Test
   fun `returns map populated with defaults if not present in input`() {
      val params = listOf(
         JdbcConnectionParam("host", PrimitiveType.STRING),
         JdbcConnectionParam("port", PrimitiveType.INTEGER, defaultValue = 5542),
         JdbcConnectionParam("database", PrimitiveType.STRING, defaultValue = "foo"),
      )
      val inputs = mapOf(
         "host" to "localhost",
         // port is missing, use the default
         "database" to "testDb" // testDb contains a default, but don't use it
      )
      val expandedParams = JdbcConnectionBuilder.assertAllParametersPresent(params, inputs)
      expandedParams.should.equal(
         mapOf(
            "host" to "localhost",
            "port" to 5542,
            "database" to "testDb"
         )
      )
   }
}
