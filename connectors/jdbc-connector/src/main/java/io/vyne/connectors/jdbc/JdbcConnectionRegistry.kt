package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

data class JdbcConnectionDetails(
   val name: String,
   val driver : JdbcDriver,
   val auth: JdbcAuthentication
)

interface JdbcAuthentication {
   // for polymorphic serialization / deserialization
   val type: Type
   enum class Type {
      UsernameAndPassword
   }
}

data class UsernameAndPasswordAuthentication(
   val username: String,
   val password: String
) {
   val kind = JdbcAuthentication.Type.UsernameAndPassword
}

interface JdbcConnectionParam

enum class JdbcDriver(
   val displayName: String,
   val driverName: String
) {
   H2(displayName = "H2", driverName = "org.h2.Driver"),
   POSTGRES(displayName = "Postgres", driverName = "org.postgresql.Driver"),
   MYSQL(displayName = "MySQL", driverName = "com.mysql.jdbc.Driver"),
}
data class JdbcConnection(val name: String, val template: NamedParameterJdbcTemplate)

class JdbcConnectionRegistry(connections: List<JdbcConnection>) {
   private val connections: MutableMap<String, JdbcConnection> = connections.associateBy { it.name }.toMutableMap()

   fun hasConnection(name: String): Boolean = connections.containsKey(name)

   fun getConnection(name: String): JdbcConnection =
      connections[name] ?: error("No JdbcConnection with name $name is registered")
}


