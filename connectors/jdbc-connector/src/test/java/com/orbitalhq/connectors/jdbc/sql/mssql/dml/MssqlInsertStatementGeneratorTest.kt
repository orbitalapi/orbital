package com.orbitalhq.connectors.jdbc.sql.mssql.dml

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration

class MssqlInsertStatementGeneratorTest {
   val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
      "postgres",
      JdbcDriver.POSTGRES,
      JdbcUrlAndCredentials("jdbc:postgresql://localhost:49229/test", "username", "password")
   )
}
