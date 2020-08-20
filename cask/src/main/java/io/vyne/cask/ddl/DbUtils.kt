package io.vyne.cask.ddl

import com.zaxxer.hikari.HikariDataSource
import org.postgresql.PGConnection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource


fun JdbcTemplate.pgConnection() = this.dataSource.pgConnection()

fun DataSource.pgConnection(): PGConnection {
    return this.connection.unwrap(PGConnection::class.java)
}
fun JdbcTemplate.enableAutoCommit() {
   this.dataSource.connection.autoCommit = true
}
fun JdbcTemplate.disableAutoCommit() {
   this.dataSource.connection.autoCommit = false
}
fun JdbcTemplate.getConnection(): Connection? {
   return this.dataSource?.connection
}
