package io.vyne.cask.ddl

import org.postgresql.PGConnection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

fun JdbcTemplate.pgConnection() = this.dataSource.pgConnection()

fun DataSource.pgConnection(): PGConnection {
    return this.connection.unwrap(PGConnection::class.java)
}
fun JdbcTemplate.getCustomConnection(): Connection? {
   val conn = this.dataSource.connection
   conn.autoCommit = false
   return conn
}

@Component
class DbPlus {
   @Autowired
   private lateinit var env: Environment

   @Bean
   fun getCustomConnection(): Connection? {
      val dataSource = DriverManagerDataSource()

      dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName")!!)
      dataSource.url = env.getProperty("spring.datasource.url")
      dataSource.username = env.getProperty("spring.datasource.username")
      dataSource.password = env.getProperty("spring.datasource.password")

      return dataSource.connection
   }
}
