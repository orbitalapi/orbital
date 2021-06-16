package io.vyne.connectors.jdbc.builders

import com.winterbe.expekt.should
import org.junit.Test

class PostgresJdbcUrlBuilderTest {

   @Test
   fun `builds connection string`() {
      val connectionString = PostgresJdbcUrlBuilder()
         .build(
            mapOf(
               "host" to "localhost",
//            port is provided by default
               "database" to "testDb",
               "username" to "jimmy",
               "password" to "secret!!"
            )
         )
      connectionString.should.equal("jdbc:postgresql://localhost:5432/testDb?username=jimmy")
   }

   @Test
   fun `builds if params have been sent as null`() {
      val connectionString = PostgresJdbcUrlBuilder()
         .build(
            mapOf(
               "host" to "localhost",
//            port is provided by default
               "database" to "testDb",
               "username" to "jimmy",
               "password" to null
            )
         )
      connectionString.should.equal("jdbc:postgresql://localhost:5432/testDb?username=jimmy&password=secret!!")
   }
}
