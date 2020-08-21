package io.vyne.cask.ddl

import org.postgresql.PGConnection
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

fun JdbcTemplate.pgConnection() = this.dataSource.pgConnection()

fun DataSource.pgConnection(): PGConnection {
    return this.connection.unwrap(PGConnection::class.java)
}
