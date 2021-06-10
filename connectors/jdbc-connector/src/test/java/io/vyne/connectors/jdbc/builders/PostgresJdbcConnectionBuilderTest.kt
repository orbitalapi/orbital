package io.vyne.connectors.jdbc.builders

import com.winterbe.expekt.should
import org.junit.Test

class PostgresJdbcConnectionBuilderTest {

   @Test
   fun `builds connection string`() {
      val connectionString = PostgresJdbcConnectionBuilder()
         .build(
            mapOf(
               "host" to "localhost",
//            port is provided by default
               "database" to "testDb",
               "username" to "jimmy",
               "password" to "secret!!"
            )
         )
      connectionString.should.equal("jdbc:postgresql://localhost:5432/testDb?username=jimmy&password=secret!!")
   }
}
