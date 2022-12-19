/**
 * TODO Remove once SchemaCrawler makes this class public
 * This is a copy of the SchemaCrawler class as it is not public. The source is from version 16.19.5 and located here:
 * https://github.com/schemacrawler/SchemaCrawler/blob/92855259a900ed8b04ead352d5f285e63ea660c3/schemacrawler-utility/src/main/java/us/fatehi/utility/datasource/DataSourceConnectionSource.java
 */

package io.vyne.connectors.jdbc.schemacrawler

import us.fatehi.utility.datasource.DatabaseConnectionSource
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
import javax.sql.DataSource

class DataSourceConnectionSource(dataSource: DataSource) :
   DatabaseConnectionSource {
   private val dataSource: DataSource

   init {
      this.dataSource = Objects.requireNonNull(dataSource, "Data source not provided")
   }

   @Throws(Exception::class)
   override fun close() {
      if (dataSource is AutoCloseable) {
         (dataSource as AutoCloseable).close()
      } else {
         val method = shutdownMethod()
         if (method != null) {
            method.isAccessible = true
            method.invoke(dataSource)
         }
      }
   }

   override fun get(): Connection {
      return try {
         val connection = dataSource.connection
         connectionInitializerProperty?.accept(connection)
         connection
      } catch (e: SQLException) {
         throw RuntimeException(e)
      }
   }

   override fun releaseConnection(connection: Connection): Boolean {
      try {
         connection.close()
      } catch (e: SQLException) {
         LOGGER.log(Level.WARNING, "Could not close database connection", e)
         return false
      }
      return true
   }

   private fun shutdownMethod(): Method? {
      val c: Class<*> = dataSource.javaClass
      val methods = c.declaredMethods
      for (method in methods) {
         val methodName = method.name
         if (methodName.equals("shutdown", ignoreCase = true)) {
            return method
         }
      }
      return null
   }

   protected var connectionInitializerProperty: Consumer<Connection>? = null

   override fun setConnectionInitializer(connectionInitializer: Consumer<Connection>?) {
      if (connectionInitializer == null) {
         this.connectionInitializerProperty = Consumer { connection: Connection -> }
      } else {
         this.connectionInitializerProperty = connectionInitializer
      }
   }

   companion object {
      private val LOGGER = Logger.getLogger(DataSourceConnectionSource::class.java.name)
   }
}
