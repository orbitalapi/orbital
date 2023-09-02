package com.orbitalhq.connectors.config.jdbc

/**
 * Another test class, useful where we want to shortcut the config, because the
 * JDBC Url has already been explitily provided to us
 */
data class JdbcUrlCredentialsConnectionConfiguration(
    override val connectionName: String,
    override val jdbcDriver: JdbcDriver,
    val urlAndCredentials: JdbcUrlAndCredentials
) : JdbcConnectionConfiguration {
   override fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder) = urlAndCredentials
}
