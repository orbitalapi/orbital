package io.vyne.connectors.calcite

import io.vyne.models.TypedInstance
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import mu.KotlinLogging
import org.apache.calcite.config.CalciteConnectionProperty
import org.apache.calcite.jdbc.CalciteConnection
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.logging.Logger
import java.util.stream.Stream
import javax.sql.DataSource


private val logger = KotlinLogging.logger {  }
class VyneCalciteDataSource(
   private val schema: Schema,
   private val fullyQualifiedName: QualifiedName,
   private val  source: Stream<TypedInstance>): DataSource {
   private val connection = vyneSqlConnection()
   private lateinit var logWriter: PrintWriter
   private var loginTimeout: Int = 30
   override fun getLogWriter(): PrintWriter {
      return logWriter
   }

   override fun setLogWriter(out: PrintWriter) {
      logWriter = out
   }

   override fun setLoginTimeout(seconds: Int) {
      loginTimeout = seconds
   }

   override fun getLoginTimeout(): Int = loginTimeout

   override fun getParentLogger(): Logger? = null

   override fun <T : Any?> unwrap(iface: Class<T>?): T = connection.unwrap(iface)

   override fun isWrapperFor(iface: Class<*>?): Boolean  = connection.isWrapperFor(iface)

   override fun getConnection(): Connection = connection

   override fun getConnection(username: String?, password: String?): Connection = connection

   private fun vyneSqlConnection(): Connection {
      val connection = DriverManager.getConnection(calciteConnection)
      val calciteConnection = connection.unwrap(CalciteConnection::class.java)
      val rootSchema = calciteConnection.rootSchema
      val calciteSchema = SingleVyneTypeCalciteSchema(schema = schema, type = schema.type(fullyQualifiedName), dataSource = source)
      rootSchema.add(schemaName, calciteSchema)
      calciteConnection.schema = schemaName
      return connection
   }

   companion object {
      private const val calciteConnection = "jdbc:calcite:"
      private const val schemaName = "vyne"
   }

}
